package de.fimatas.home.controller.domain.service;

import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.service.*;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.*;
import de.fimatas.home.library.homematic.model.Type;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.model.PhotovoltaicsStringsStatus;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import mfi.files.api.UserService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@CommonsLog
public class HouseService {

    private static final BigDecimal TARGET_TEMPERATURE_INSIDE = new BigDecimal("21");

    private static final BigDecimal TARGET_TEMPERATURE_TOLERANCE_OFFSET = new BigDecimal("1");

    private static final BigDecimal TEMPERATURE_DIFFERENCE_INSIDE_OUTSIDE_NO_ROOM_COOLDOWN_NEEDED = new BigDecimal("6");

    private static final BigDecimal TEMPERATURE_TENDENCY_DIFF = new BigDecimal("0.199");

    private static final BigDecimal HUMIDITY_TENDENCY_DIFF = new BigDecimal("1.99");

    private static final BigDecimal POWER_TENDENCY_DIFF = new BigDecimal("99.99");

    private static final BigDecimal SUN_INTENSITY_NO = new BigDecimal("3");

    private static final BigDecimal SUN_INTENSITY_LOW = new BigDecimal("8");

    private static final BigDecimal SUN_INTENSITY_MEDIUM = new BigDecimal("15");

    public static final String AUTOMATIC = "Automatic";

    private static final String EVENT = "Event";

    private static final String BUSY = "Busy";

    private static final String IS_OPENED = "IsOpened";

    private static final String TIMESTAMP = "Timestamp";

    @Autowired
    private HomematicAPI hmApi;

    @Autowired
    private CameraService cameraService;

    @Autowired
    private PushService pushService;

    @Autowired
    private LiveActivityService liveActivityService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private UserService userService;

    @Autowired
    private ElectricVehicleService electricVehicleService;

    @Autowired
    private PhotovoltaicsOverflowService photovoltaicsOverflowService;

    @Autowired
    private SolarmanService solarmanService;

    @Autowired
    private Environment env;

    @Scheduled(initialDelay = (1000 * 3), fixedDelay = (1000 * HomeAppConstants.MODEL_DEFAULT_INTERVAL_SECONDS))
    private void scheduledRefreshHouseModel() {
        refreshHouseModel();
    }

    public synchronized void refreshHouseModel() {

        HouseModel oldModel = ModelObjectDAO.getInstance().readHouseModel();
        HouseModel newModel = refreshModel(oldModel);
        if (newModel == null) {
            return;
        }

        calculateConclusion(oldModel, newModel);
        ModelObjectDAO.getInstance().write(newModel);

        calculateHints(oldModel, newModel);

        pushService.sendAfterModelRefresh(oldModel, newModel);
        uploadService.uploadToClient(newModel);
        liveActivityService.newModel(newModel);
        uploadService.uploadToAdapter(newModel);

        // updateCameraPictures(oldModel, newModel); // async
        updateHomematicSystemVariables(oldModel, newModel);
        cameraService.cleanUp();

        historyService.saveNewValues();

        informOtherServices(oldModel, newModel);
    }

    private HouseModel refreshModel(HouseModel oldModel) {

        if (!hmApi.refresh()) {
            return null;
        }

        HouseModel newModel = new HouseModel();

        newModel.setClimateBathRoom(readRoomClimate(Device.THERMOSTAT_BAD));
        newModel.setHeatingBathRoom(readHeating(Device.THERMOSTAT_BAD));
        newModel.setClimateKidsRoom1(readRoomClimate(Device.THERMOMETER_KINDERZIMMER_1));
        newModel.setClimateKidsRoom2(readRoomClimate(Device.THERMOMETER_KINDERZIMMER_2));
        newModel.setClimateLivingRoom(readRoomClimate(Device.THERMOMETER_WOHNZIMMER));
        newModel.setClimateBedRoom(readRoomClimate(Device.THERMOMETER_SCHLAFZIMMER));
        newModel.setClimateLaundry(readRoomClimate(Device.THERMOMETER_WASCHKUECHE));

        newModel.setClimateGuestRoom(readRoomClimate(Device.THERMOMETER_GAESTEZIMMER));
        newModel.setHeatingGuestRoom(readHeating(Device.THERMOSTAT_GAESTEZIMMER));
        newModel.setGuestRoomWindowSensor(readWindowSensorState(Device.FENSTERSENSOR_GAESTEZIMMER));

        newModel.setClimateWorkshop(readRoomClimate(Device.THERMOMETER_WERKSTATT));
        newModel.setWorkshopWindowSensor(readWindowSensorState(Device.FENSTERSENSOR_WERKSTATT));
        newModel.setLaundryWindowSensor(readWindowSensorState(Device.FENSTERSENSOR_WASCHKUECHE));

        // newModel.setLeftWindowBedRoom(readWindow(Device.ROLLLADE_SCHLAFZIMMER_LINKS)); // NOSONAR

        newModel.setClimateEntrance(
            readOutdoorClimate(Device.THERMOMETER_EINFAHRT, Device.THERMOMETER_GARTEN));
        newModel.setClimateGarden(readOutdoorClimate(Device.THERMOMETER_GARTEN, Device.THERMOMETER_EINFAHRT));

        newModel.setKitchenWindowLightSwitch(readSwitchState(Device.SCHALTER_KUECHE_LICHT));
        newModel.setWallboxSwitch(readSwitchState(Device.SCHALTER_WALLBOX));
        newModel.setWorkshopVentilationSwitch(readSwitchState(Device.SCHALTER_WERKSTATT_LUEFTUNG));
        newModel.setGuestRoomInfraredHeater(readSwitchState(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG));
        newModel.setWorkshopLightSwitch(readSwitchState(Device.SCHALTER_LICHT_WERKSTATT));

        newModel.setFrontDoorBell(readFrontDoorBell());
        newModel.setFrontDoorCamera(readFrontDoorCamera());
        newModel.setFrontDoorLock(readFrontDoorLock(oldModel));

        newModel.setGridElectricalPower(readPowerConsumption(Device.STROMZAEHLER_BEZUG));
        newModel.setProducedElectricalPower(readPowerConsumption(Device.ELECTRIC_POWER_PRODUCTION_ACTUAL_HOUSE));
        newModel.setConsumedElectricalPower(readPowerConsumption(Device.ELECTRIC_POWER_CONSUMPTION_ACTUAL_HOUSE));
        newModel.setPvStatusTime(formatTimestamp(Device.ELECTRIC_POWER_ACTUAL_TIMESTAMP_HOUSE));
        newModel.setGridElectricStatusTime(formatTimestamp(Device.ELECTRIC_POWER_GRID_TIMESTAMP_HOUSE));

        newModel.setWallboxElectricalPowerConsumption(readPowerConsumption(Device.STROMZAEHLER_WALLBOX));
        newModel.setGasConsumption(readPowerConsumption(Device.GASZAEHLER));

        // associated devices
        newModel.getWallboxSwitch().setAssociatedPowerMeter(newModel.getWallboxElectricalPowerConsumption());

        for (Device device : Device.values()) {
            checkLowBattery(newModel, device);
        }

        ckeckWarnings(newModel);
        readSubtitles(newModel);

        return newModel;
    }

    private long formatTimestamp(Device device) {
        try{
            return Long.parseLong(hmApi.getAsString(homematicCommandBuilder.read(device, Datapoint.SYSVAR_DUMMY))) * 1000L;
        }catch (Exception e){
            log.warn("PV timestamp problem.", e);
            return 0;
        }
    }

    private void informOtherServices(HouseModel oldModel, HouseModel newModel) {
        if(oldModel==null || (oldModel.getWallboxSwitch().isState() != newModel.getWallboxSwitch().isState())){
            electricVehicleService.startNewChargingEntryAndRefreshModel();
        }
        photovoltaicsOverflowService.houseModelRefreshed();
    }

    private void calculateConclusion(HouseModel oldModel, HouseModel newModel) {

        List<OutdoorClimate> outdoor = Stream.of(//
            newModel.getClimateEntrance(), newModel.getClimateGarden() //
        ).filter(c -> !c.isUnreach()).collect(Collectors.toList());

        calculateOutdoorMinMax(newModel, outdoor);
        calculateTendencies(oldModel, newModel);
    }

    private void calculateOutdoorMinMax(HouseModel newModel, List<OutdoorClimate> outdoor) {

        Comparator<OutdoorClimate> comparator =
            Comparator.comparing(OutdoorClimate::getTemperature, Comparator.comparing(ValueWithTendency::getValue));
        Optional<OutdoorClimate> minTemperature = outdoor.stream().min(comparator);
        Optional<OutdoorClimate> maxTemperature = outdoor.stream().max(comparator);

        // compensating absent difference temperature value
        if (minTemperature.isPresent()) {
            newModel.setConclusionClimateFacadeMin(SerializationUtils.clone(minTemperature.get()));
            newModel.getConclusionClimateFacadeMin().setDevice(Device.AUSSENTEMPERATUR);
            newModel.getConclusionClimateFacadeMin().setBase(minTemperature.get().getDevice());
        } else {
            var empty = new OutdoorClimate();
            empty.setDevice(Device.AUSSENTEMPERATUR);
            empty.setUnreach(true);
            newModel.setConclusionClimateFacadeMin(empty);
        }

        if (maxTemperature.isPresent()) {
            if (minTemperature.isPresent()) {
                BigDecimal diffOutside = maxTemperature.get().getTemperature().getValue()
                    .subtract(minTemperature.get().getTemperature().getValue()).abs();
                maxTemperature.get().setSunBeamIntensity(lookupIntensity(diffOutside));
            }
            newModel.setConclusionClimateFacadeMax(SerializationUtils.clone(maxTemperature.get()));
        } else {
            var empty = new OutdoorClimate();
            empty.setUnreach(true);
            newModel.setConclusionClimateFacadeMax(empty);
        }
    }

    void calculateTendencies(HouseModel oldModel, HouseModel newModel) {

        Map<String, Climate> places = newModel.lookupFields(Climate.class);

        // Climate
        for (Entry<String, Climate> entry : places.entrySet()) {
            Climate climateNew = entry.getValue();
            Climate climateOld = oldModel != null ? oldModel.lookupField(entry.getKey(), Climate.class) : null;
            calculateClimateTendencies(newModel, climateNew, climateOld);
        }

        // Power consumption
        calculatePowerConsumptionTendencies(newModel.getDateTime(),
            oldModel == null ? null : oldModel.getGridElectricalPower(),
            newModel.getGridElectricalPower());
        calculatePowerConsumptionTendencies(newModel.getDateTime(),
                oldModel == null ? null : oldModel.getProducedElectricalPower(),
                newModel.getProducedElectricalPower());
        calculatePowerConsumptionTendencies(newModel.getDateTime(),
                oldModel == null ? null : oldModel.getConsumedElectricalPower(),
                newModel.getConsumedElectricalPower());
        calculatePowerConsumptionTendencies(newModel.getDateTime(),
            oldModel == null ? null : oldModel.getWallboxElectricalPowerConsumption(),
            newModel.getWallboxElectricalPowerConsumption());
    }

    private void calculatePowerConsumptionTendencies(long newModelDateTime, PowerMeter oldModel, PowerMeter newModel) {

        if (newModel == null || newModel.isUnreach()) {
            return;
        }

        ValueWithTendency<BigDecimal> referencePower;
        if (oldModel == null || oldModel.isUnreach()) {
            referencePower = newModel.getActualConsumption();
            referencePower.setReferenceValue(referencePower.getValue());
        } else {
            referencePower = oldModel.getActualConsumption();
        }
        calculateTendency(newModelDateTime, referencePower, newModel.getActualConsumption(), POWER_TENDENCY_DIFF);
    }

    private void calculateClimateTendencies(HouseModel newModel, Climate climateNew, Climate climateOld) {

        if (climateNew == null || climateNew.isUnreach()) {
            return;
        }

        // Temperature
        if (climateNew.getTemperature() != null) {
            ValueWithTendency<BigDecimal> referenceTemperature;
            if (climateOld == null || climateOld.isUnreach()) {
                referenceTemperature = climateNew.getTemperature();
                referenceTemperature.setReferenceValue(referenceTemperature.getValue());
            } else {
                referenceTemperature = climateOld.getTemperature();
            }
            calculateTendency(newModel.getDateTime(), referenceTemperature, climateNew.getTemperature(),
                TEMPERATURE_TENDENCY_DIFF);
        }

        // Humidity
        if (climateNew.getHumidity() != null) {
            ValueWithTendency<BigDecimal> referenceHumidity;
            if (climateOld == null || climateOld.isUnreach()) {
                referenceHumidity = climateNew.getHumidity();
                referenceHumidity.setReferenceValue(referenceHumidity.getValue());
            } else {
                referenceHumidity = climateOld.getHumidity();
            }
            calculateTendency(newModel.getDateTime(), referenceHumidity, climateNew.getHumidity(), HUMIDITY_TENDENCY_DIFF);
        }
    }

    private void calculateTendency(long newTimestamp, ValueWithTendency<BigDecimal> reference,
            ValueWithTendency<BigDecimal> actual, BigDecimal diffValue) {

        if (actual.getValue() == null || reference == null || reference.getReferenceValue() == null) {
            actual.setTendency(Tendency.NONE);
            return;
        }

        BigDecimal diff = actual.getValue().subtract(reference.getReferenceValue());

        if (diff.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(diffValue) > 0) {
            actual.setTendency(Tendency.RISE);
            actual.setReferenceValue(actual.getValue());
            actual.setReferenceDateTime(newTimestamp);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0 && diff.abs().compareTo(diffValue) > 0) {
            actual.setTendency(Tendency.FALL);
            actual.setReferenceValue(actual.getValue());
            actual.setReferenceDateTime(newTimestamp);
        } else {
            long timeDiff = newTimestamp - reference.getReferenceDateTime();
            actual.setTendency(Tendency.calculate(reference, timeDiff));
            actual.setReferenceValue(reference.getReferenceValue());
            actual.setReferenceDateTime(reference.getReferenceDateTime());
        }
    }

    public void calculateHints(HouseModel oldModel, HouseModel newModel) {

        try {
            lookupHint(oldModel != null ? oldModel.getClimateKidsRoom1() : null, newModel.getClimateKidsRoom1(), null,
                newModel.getClimateEntrance(), newModel.getDateTime());
            lookupHint(oldModel != null ? oldModel.getClimateKidsRoom2() : null, newModel.getClimateKidsRoom2(), null,
                    newModel.getClimateEntrance(), newModel.getDateTime());
            lookupHint(oldModel != null ? oldModel.getClimateBathRoom() : null, newModel.getClimateBathRoom(),
                newModel.getHeatingBathRoom(), newModel.getClimateGarden(), newModel.getDateTime());
            lookupHint(oldModel != null ? oldModel.getClimateBedRoom() : null, newModel.getClimateBedRoom(), null,
                newModel.getClimateGarden(), newModel.getDateTime());
            lookupHint(oldModel != null ? oldModel.getClimateLivingRoom() : null, newModel.getClimateLivingRoom(), null,
                newModel.getClimateGarden(), newModel.getDateTime());
            lookupHint(oldModel != null ? oldModel.getClimateLaundry() : null, newModel.getClimateLaundry(), null, null,
                newModel.getDateTime());
        } catch (RuntimeException re) {
            LogFactory.getLog(HouseService.class).error("Could not calculate hints:", re);
        }
    }

    private void lookupHint(RoomClimate old, RoomClimate room, Heating heating, OutdoorClimate outdoor, long dateTime) {
        if (old != null && old.getHints() != null) {
            room.getHints().overtakeOldHints(old.getHints(), dateTime);
        }
        if (outdoor != null && !room.isUnreach() && !outdoor.isUnreach() && (heating==null || !heating.isUnreach())) {
            lookupTemperatureHint(room, heating, outdoor, dateTime);
        }
        lookupHumidityHint(room, dateTime);
    }

    private void lookupHumidityHint(RoomClimate room, long dateTime) {

        if (room.getHumidity() == null) {
            return;
        }

        if (room.getHumidity().getValue().compareTo(HomeAppConstants.TARGET_HUMIDITY_MAX_INSIDE) > 0) {
            room.getHints().giveHint(Hint.DECREASE_HUMIDITY, dateTime);
        } else if (room.getHumidity().getValue().compareTo(HomeAppConstants.TARGET_HUMIDITY_MIN_INSIDE) < 0) {
            room.getHints().giveHint(Hint.INCREASE_HUMIDITY, dateTime);
        }
    }

    private void lookupTemperatureHint(RoomClimate room, Heating heating, OutdoorClimate outdoor, long dateTime) {

        BigDecimal targetTemperature =
            heating != null && !heating.isUnreach() ? heating.getTargetTemperature() : TARGET_TEMPERATURE_INSIDE;
        BigDecimal temperatureLimit = targetTemperature.add(TARGET_TEMPERATURE_TOLERANCE_OFFSET);

        if (noTemperatureAvailable(room)) {
            // nothing to do
        } else if (temperatueIsOkay(room, temperatureLimit)) {
            // using sun heating in the winter for warming up rooms
        } else if (isTooColdOutsideSoNoNeedToCoolingDownRoom(room.getTemperature().getValue())) {
            // no hint
        } else if (temperatureOutsideColderThanInside(room, outdoor, temperatureLimit)) {
            if (isHeatingIsCauseForHighRoomTemperature(heating, temperatureLimit)) {
                // no hint
            } else {
                if (room.getDevice().getPlace().isAirCondition()) {
                    room.getHints().giveHint(Hint.TURN_ON_AIRCONDITION, dateTime);
                } else {
                    room.getHints().giveHint(Hint.OPEN_WINDOW, dateTime);
                }
            }
        } else if (temperatureInsideColderThanOutside(room, outdoor, temperatureLimit)) {
            room.getHints().giveHint(Hint.CLOSE_ROLLER_SHUTTER, dateTime);
        }
    }

    private boolean temperatureInsideColderThanOutside(RoomClimate room, OutdoorClimate outdoor, BigDecimal temperatureLimit) {
        return room.getTemperature().getValue().compareTo(temperatureLimit) > 0
            && outdoor.getSunBeamIntensity().ordinal() > Intensity.LOW.ordinal();
    }

    private boolean temperatureOutsideColderThanInside(RoomClimate room, OutdoorClimate outdoor, BigDecimal temperatureLimit) {
        return room.getTemperature().getValue().compareTo(temperatureLimit) > 0
            && outdoor.getTemperature().getValue().compareTo(room.getTemperature().getValue()) < 0
            && outdoor.getSunBeamIntensity().ordinal() <= Intensity.LOW.ordinal();
    }

    private boolean temperatueIsOkay(RoomClimate room, BigDecimal temperatureLimit) {
        return room.getTemperature().getValue().compareTo(temperatureLimit) < 0;
    }

    private boolean noTemperatureAvailable(RoomClimate room) {
        return room.getTemperature() == null || room.getTemperature().getValue() == null;
    }

    private boolean isHeatingIsCauseForHighRoomTemperature(Heating heating, BigDecimal temperatureLimit) {
        return heating != null && (heating.isBoostActive() || heating.getTargetTemperature().compareTo(temperatureLimit) > 0);
    }

    private boolean isTooColdOutsideSoNoNeedToCoolingDownRoom(BigDecimal roomTemperature) {

        if (ModelObjectDAO.getInstance().readHistoryModel() == null
            || ModelObjectDAO.getInstance().readHistoryModel().getHighestOutsideTemperatureInLast24Hours() == null) {
            LogFactory.getLog(HouseService.class).debug("HighestOutsideTemperatureInLast24Hours == null");
            return true;
        }

        BigDecimal roomMinusOutside = roomTemperature
            .subtract(ModelObjectDAO.getInstance().readHistoryModel().getHighestOutsideTemperatureInLast24Hours());
        return roomMinusOutside.compareTo(TEMPERATURE_DIFFERENCE_INSIDE_OUTSIDE_NO_ROOM_COOLDOWN_NEEDED) > 0;
    }

    private Intensity lookupIntensity(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(SUN_INTENSITY_NO) < 0) {
            return Intensity.NO;
        } else if (value.compareTo(SUN_INTENSITY_LOW) < 0) {
            return Intensity.LOW;
        } else if (value.compareTo(SUN_INTENSITY_MEDIUM) < 0) {
            return Intensity.MEDIUM;
        } else {
            return Intensity.HIGH;
        }
    }

    public void togglestate(Device device, boolean value) {
        hmApi.executeCommand(homematicCommandBuilder.write(device, Datapoint.STATE, value));
    }

    public void toggleautomation(Device device, AutomationState value) {

        boolean event = false;
        switch (value) {
        case MANUAL:
            hmApi.executeCommand(homematicCommandBuilder.write(device, AUTOMATIC, false));
            break;
        case AUTOMATIC:
            hmApi.executeCommand(homematicCommandBuilder.write(device, AUTOMATIC, true));
            break;
        case AUTOMATIC_PLUS_EVENT:
            hmApi.executeCommand(homematicCommandBuilder.write(device, AUTOMATIC, true));
            event = true;
            break;
        }
        HomematicCommand eventCommand = homematicCommandBuilder.read(device, AUTOMATIC + EVENT);
        if (hmApi.isPresent(eventCommand)) {
            hmApi.executeCommand(homematicCommandBuilder.write(device, AUTOMATIC + EVENT, event));
        }
    }

    public void heatingBoost(Device device) {
        runProgram(device, "Boost");
    }

    // needs to be synchronized because of using ccu-systemwide temperature
    // variable
    public synchronized void heatingManual(Device device, BigDecimal temperature) {
        hmApi.executeCommand(homematicCommandBuilder.write(device, "Temperature", temperature.toString()));
        runProgram(device, "Manual");
    }

    public synchronized void heatingAuto(Device device) {
        runProgram(device, "Auto");
    }

    public void doorState(Message message) {
        if (StringUtils.isNotBlank(message.getSecurityPin())
            && userService.checkPin(message.getUser(), message.getSecurityPin())) {
            switch (StateValue.valueOf(message.getValue())) {
            case LOCK:
                runProgram(message.getDevice(), "Lock");
                break;
            case UNLOCK:
                runProgram(message.getDevice(), "Unlock");
                break;
            case OPEN:
                runProgram(message.getDevice(), "Open");
                break;
            default:
                // noop
            }
        }
    }

    private void runProgram(Device device, String programSuffix) {
        hmApi.executeCommand(homematicCommandBuilder.write(device, BUSY, String.valueOf(System.currentTimeMillis())),
            homematicCommandBuilder.exec(device, programSuffix));
    }

    private void updateHomematicSystemVariables(HouseModel oldModel, HouseModel newModel) {

        if (newValueForOutdoorTemperature(oldModel, newModel)) {
            hmApi.executeCommand(homematicCommandBuilder.write(newModel.getConclusionClimateFacadeMin().getDevice(),
                Datapoint.SYSVAR_DUMMY, newModel.getConclusionClimateFacadeMin().getTemperature().getValue().toString()));
        }
    }

    private boolean newValueForOutdoorTemperature(HouseModel oldModel, HouseModel newModel) {

        BigDecimal oldVal =
            oldModel != null && oldModel.getConclusionClimateFacadeMin() != null
                && !oldModel.getConclusionClimateFacadeMin().isUnreach()
                && oldModel.getConclusionClimateFacadeMin().getTemperature() != null
                    ? oldModel.getConclusionClimateFacadeMin().getTemperature().getValue() : null;

        BigDecimal newVal =
            newModel.getConclusionClimateFacadeMin() != null && !newModel.getConclusionClimateFacadeMin().isUnreach()
                && newModel.getConclusionClimateFacadeMin().getTemperature() != null
                    ? newModel.getConclusionClimateFacadeMin().getTemperature().getValue() : null;

        return newVal != null && (oldVal == null || oldVal.compareTo(newVal) != 0);
    }

    public static boolean doorbellTimestampChanged(HouseModel oldModel, HouseModel newModel) {

        long doorbellOld = oldModel != null && oldModel.getFrontDoorBell() != null
            && oldModel.getFrontDoorBell().getTimestampLastDoorbell() != null
                ? oldModel.getFrontDoorBell().getTimestampLastDoorbell() : 0;
        long doorbellNew = newModel != null && newModel.getFrontDoorBell() != null
            && newModel.getFrontDoorBell().getTimestampLastDoorbell() != null
                ? newModel.getFrontDoorBell().getTimestampLastDoorbell() : 0;

        long minutesAgo = (System.currentTimeMillis() - doorbellNew) / 1000 / 60;

        return doorbellOld != doorbellNew && doorbellNew > 0 && oldModel != null && minutesAgo < 10;
    }

    private OutdoorClimate readOutdoorClimate(Device outside, Device diff) {

        OutdoorClimate outdoorClimate = new OutdoorClimate();
        outdoorClimate.setDevice(outside);
        outdoorClimate.setUnreach(hmApi.isDeviceUnreachableOrNotSending(outside));
        if (outdoorClimate.isUnreach()) {
            return outdoorClimate;
        }

        outdoorClimate.setTemperature(new ValueWithTendency<>(hmApi.getAsBigDecimal(homematicCommandBuilder.read(outside,
            outside.isHomematicIP() ? Datapoint.ACTUAL_TEMPERATURE : Datapoint.TEMPERATURE))));

        HomematicCommand humidityCommand = homematicCommandBuilder.read(outside, Datapoint.HUMIDITY);
        BigDecimal humidity = hmApi.isPresent(humidityCommand) ? hmApi.getAsBigDecimal(humidityCommand) : null;
        if (humidity != null) {
            outdoorClimate.setHumidity(new ValueWithTendency<>(humidity));
        }

        if (diff != null && !hmApi.isDeviceUnreachableOrNotSending(diff)) {
            outdoorClimate.setSunBeamIntensity(
                lookupIntensity(outdoorClimate.getTemperature().getValue()
                        .subtract(hmApi.getAsBigDecimal(homematicCommandBuilder
                                .read(diff, diff.isHomematicIP() ? Datapoint.ACTUAL_TEMPERATURE : Datapoint.TEMPERATURE)))));
        }else{
            outdoorClimate.setSunBeamIntensity(Intensity.NO);
        }

        return outdoorClimate;
    }

    private RoomClimate readRoomClimate(Device thermometer) {

        RoomClimate roomClimate = new RoomClimate();
        roomClimate.setDevice(thermometer);
        roomClimate.setUnreach(hmApi.isDeviceUnreachableOrNotSending(thermometer));
        if (roomClimate.isUnreach()) {
            return roomClimate;
        }

        roomClimate.setTemperature(new ValueWithTendency<>(
            hmApi.getAsBigDecimal(homematicCommandBuilder.read(thermometer, Datapoint.ACTUAL_TEMPERATURE))));
        BigDecimal humidity = thermometer.getType() == Type.THERMOSTAT ? null
            : hmApi.getAsBigDecimal(homematicCommandBuilder.read(thermometer, Datapoint.HUMIDITY));
        if (humidity != null) {
            roomClimate.setHumidity(new ValueWithTendency<>(humidity));
        }
        if (thermometer.getType() != Type.THERMOMETER) {
            roomClimate.setSubType(Type.THERMOMETER);
        }
        return roomClimate;
    }

    private void readSubtitles(HouseModel houseModel) {
        for (Place place : Place.values()) {
            Optional<String> subtitle = readSubtitleFor(place);
            subtitle.ifPresent(s -> houseModel.getPlaceSubtitles().put(place, s));
        }
    }

    public Optional<String> readSubtitleFor(Place place){
        var key = "place." + place.name() + ".subtitle";
        if(env.containsProperty(key)){
            return Optional.of(env.getProperty(key));
        }
        return Optional.empty();
    }

    private Heating readHeating(Device heating) {

        Heating heatingModel = new Heating();
        heatingModel.setDevice(heating);
        heatingModel.setUnreach(hmApi.isDeviceUnreachableOrNotSending(heating));
        if (heatingModel.isUnreach()) {
            return heatingModel;
        }
        heatingModel.setBusy(checkBusyState(heating));

        boolean boost;
        boolean auto;
        boolean manual;
        int boostMinutesLeft;
        BigDecimal targetTemperature;

        if(heating.isHomematicIP()){
            boost = hmApi.getAsBoolean(homematicCommandBuilder.read(heating, Datapoint.BOOST_MODE));
            auto = !boost && hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.SET_POINT_MODE))
                    .compareTo(HomematicConstants.HEATING_CONTROL_MODE_AUTO) == 0;
            manual = !boost && hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.SET_POINT_MODE))
                    .compareTo(HomematicConstants.HEATING_CONTROL_MODE_MANUAL) == 0;
            boostMinutesLeft = NumberUtils.toInt(hmApi.getAsString(homematicCommandBuilder.read(heating, Datapoint.BOOST_TIME)), 0) / 60;
            targetTemperature = hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.SET_POINT_TEMPERATURE));
        }else{
            boost = hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.CONTROL_MODE))
                    .compareTo(HomematicConstants.HEATING_CONTROL_MODE_BOOST) == 0;
            auto = hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.CONTROL_MODE))
                    .compareTo(HomematicConstants.HEATING_CONTROL_MODE_AUTO) == 0;
            manual = hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.CONTROL_MODE))
                    .compareTo(HomematicConstants.HEATING_CONTROL_MODE_MANUAL) == 0;
            boostMinutesLeft = NumberUtils.toInt(hmApi.getAsString(homematicCommandBuilder.read(heating, Datapoint.BOOST_STATE)), 0);
            targetTemperature = hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.SET_TEMPERATURE));
        }

        heatingModel.setBoostActive(boost);
        heatingModel.setAutoActive(auto);
        heatingModel.setManualActive(manual);
        heatingModel.setBoostMinutesLeft(boostMinutesLeft);
        heatingModel.setTargetTemperature(targetTemperature);

        return heatingModel;
    }

    private boolean checkBusyState(Device device) {

        String busyString = hmApi.getAsString(homematicCommandBuilder.read(device, BUSY));
        if (StringUtils.isNotBlank(busyString) && StringUtils.isNumeric(busyString)) {
            long busyTimestamp = Long.parseLong(busyString);
            long diff = System.currentTimeMillis() - busyTimestamp;
            if (diff < 1000 * 60 * 3) {
                return true;
            }
            hmApi.executeCommand(homematicCommandBuilder.write(device, BUSY, StringUtils.EMPTY));
        }
        return false;
    }

    @SuppressWarnings("unused")
    private Shutter readShutter(Device shutterDevice) {
        Shutter shutter = new Shutter();
        shutter.setDevice(shutterDevice);
        shutter.setUnreach(hmApi.isDeviceUnreachableOrNotSending(shutterDevice));
        if (shutter.isUnreach()) {
            return shutter;
        }

        shutter.setShutterPositionPercentage(30);
        shutter.setShutterPosition(ShutterPosition.fromPosition(shutter.getShutterPositionPercentage()));
        shutter.setShutterAutomation(true);
        shutter.setShutterAutomationInfoText("Dummy Text");
        return shutter;
    }

    public void shutterPosition(Device device, int parseInt) {
        // for further use
    }

    private Switch readSwitchState(Device device) {
        Switch switchModel = new Switch();
        readSwitchInternal(device, switchModel);
        return switchModel;
    }

    private void readSwitchInternal(Device device, Switch switchModel) {
        switchModel.setDevice(device);
        switchModel.setUnreach(hmApi.isDeviceUnreachableOrNotSending(device));
        if (switchModel.isUnreach()) {
            return;
        }

        switchModel.setState(hmApi.getAsBoolean(homematicCommandBuilder.read(device, Datapoint.STATE)));
        if(device.getSysVars() != null && device.getSysVars().contains(AUTOMATIC)){
            switchModel.setAutomation(hmApi.getAsBoolean(homematicCommandBuilder.read(device, AUTOMATIC)));
            switchModel.setAutomationInfoText(hmApi.getAsString(homematicCommandBuilder.read(device, AUTOMATIC + "InfoText")));
        }
    }

    private WindowSensor readWindowSensorState(Device device) {

        WindowSensor windowSensorModel = new WindowSensor();
        windowSensorModel.setDevice(device);
        windowSensorModel.setUnreach(hmApi.isDeviceUnreachableOrNotSending(device));
        if (windowSensorModel.isUnreach()) {
            return windowSensorModel;
        }

        BigDecimal decimalState = hmApi.getAsBigDecimal(homematicCommandBuilder.read(device, Datapoint.STATE));
        int stateInt = decimalState == null ? 0 : decimalState.intValue();
        windowSensorModel.setState(BooleanUtils.toBoolean(stateInt));

        String ts = hmApi.getAsString(homematicCommandBuilder.read(device, TIMESTAMP));
        if (StringUtils.isNumeric(ts)) {
            windowSensorModel.setStateTimestamp(Long.parseLong(ts) * 1000);
        }

        return windowSensorModel;
    }

    private Doorbell readFrontDoorBell() {

        Doorbell frontDoor = new Doorbell();
        frontDoor.setDevice(Device.HAUSTUER_KLINGEL);
        frontDoor.setUnreach(hmApi.isDeviceUnreachableOrNotSending(Device.HAUSTUER_KLINGEL));
        if (frontDoor.isUnreach()) {
            return frontDoor;
        }

        String ts = hmApi.getAsString(homematicCommandBuilder.read(Device.HAUSTUER_KLINGEL, TIMESTAMP));
        if (StringUtils.isNumeric(ts)) {
            frontDoor.setTimestampLastDoorbell(Long.parseLong(ts) * 1000);
        }

        return frontDoor;
    }

    private Camera readFrontDoorCamera() {

        Camera frontDoor = new Camera();
        frontDoor.setDevice(null); // for further use
        return frontDoor;
    }

    private Doorlock readFrontDoorLock(HouseModel oldModel) {

        Doorlock frontDoor = new Doorlock();

        frontDoor.setDevice(Device.HAUSTUER_SCHLOSS);
        frontDoor.setUnreach(hmApi.isDeviceUnreachableOrNotSending(Device.HAUSTUER_SCHLOSS));
        if (frontDoor.isUnreach()) {
            return frontDoor;
        }

        boolean newBusy = checkBusyState(Device.HAUSTUER_SCHLOSS);
        frontDoor.setBusy(newBusy);
        frontDoor.setLockState(!hmApi.getAsBoolean(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, Datapoint.STATE))); // false=verriegelt
        frontDoor.setLockStateUncertain(
            hmApi.getAsBoolean(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, Datapoint.STATE_UNCERTAIN)));
        frontDoor.setOpen(hmApi.getAsBoolean(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, IS_OPENED)));

        if (oldModel != null && oldModel.getFrontDoorLock().isBusy() && !newBusy &&
                doorLockHash(oldModel.getFrontDoorLock()) == doorLockHash(frontDoor)) {
            frontDoor.setLockStateUncertain(true);
        }

        BigDecimal errorCode = hmApi.getAsBigDecimal(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, Datapoint.ERROR));
        frontDoor.setErrorcode(errorCode == null ? 0 : errorCode.intValue());

        frontDoor.setLockAutomation(hmApi.getAsBoolean(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, AUTOMATIC)));
        frontDoor
            .setLockAutomationEvent(hmApi.getAsBoolean(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, AUTOMATIC + EVENT)));
        frontDoor.setLockAutomationInfoText(
            hmApi.getAsString(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, AUTOMATIC + "InfoText")));

        return frontDoor;
    }

    private int doorLockHash(Doorlock doorlock) { // compoment sends delayed state change
        int hash = 7;
        hash = 31 * hash + (doorlock.isLockState() ? 1 : 0);
        hash = 31 * hash + (doorlock.isLockStateUncertain() ? 1 : 0);
        hash = 31 * hash + (doorlock.isOpen() ? 1 : 0);
        hash = 31 * hash + (doorlock.getErrorcode() == 0 ? 0 : 1);
        return hash;
    }

    private PowerMeter readPowerConsumption(Device device) {

        PowerMeter model = new PowerMeter();
        model.setDevice(device);
        model.setUnreach(hmApi.isDeviceUnreachableOrNotSending(device));
        if (model.isUnreach()) {
            return model;
        }

        if (isPowerConsumptionOutdated(device)) {
            model.setActualConsumption(new ValueWithTendency<>(BigDecimal.ZERO));
        } else {
            Datapoint datapoint;
            if(device.getType() == Type.GAS_POWER){
                datapoint = Datapoint.GAS_POWER;
            }else if(device.getDatapoints().contains(Datapoint.POWER)){
                datapoint = Datapoint.POWER;
            }else if(device.isSysVar()){
                datapoint = Datapoint.SYSVAR_DUMMY;
            }else{
                datapoint = Datapoint.IEC_POWER;
            }

            model.setActualConsumption(
                new ValueWithTendency<>(hmApi.getAsBigDecimal(homematicCommandBuilder.read(device, datapoint))));
        }

        return model;
    }

    private boolean isPowerConsumptionOutdated(Device device) {

        if(device.isSysVar()){
            return false;
        }

        String ts = hmApi.getAsString(homematicCommandBuilder.read(device, TIMESTAMP));
        if (!StringUtils.isNumeric(ts)) {
            return false;
        }

        long tsLong = Long.parseLong(ts) * 1000;
        LocalDateTime lastValueChange = Instant.ofEpochMilli(tsLong).atZone(ZoneId.systemDefault()).toLocalDateTime();

        return Duration.between(lastValueChange, LocalDateTime.now())
            .toSeconds() > (device.getType() == Type.GAS_POWER ? HomeAppConstants.POWER_CONSUMPTION_OUTDATED_DECONDS_GAS : HomeAppConstants.POWER_CONSUMPTION_OUTDATED_DECONDS_ELECTRICITY);
    }

    private void checkLowBattery(HouseModel model, Device device) {

        if (!device.getType().isHasBattery()) {
            return;
        }

        boolean state = hmApi.getAsBoolean(homematicCommandBuilder.read(device, device.lowBatDatapoint()));
        if (state) {
            model.getLowBatteryDevices().add(device.getDescription());
        }
    }

    private void ckeckWarnings(HouseModel newModel) {

        if (BooleanUtils.isFalse(hmApi.getCcuAuthActive())) {
            newModel.getWarnings().add("CCU Authentifizierung ist nicht aktiv!");
        }

        if(solarmanService.getStringsStatus() == PhotovoltaicsStringsStatus.ERROR_DETECTING){
            newModel.getWarnings().add("Status der Photovoltaikanlage konnte nicht geprüft werden.");
        } else if(solarmanService.getStringsStatus() == PhotovoltaicsStringsStatus.ONE_FAULTY){
            newModel.getWarnings().add("Teilausfall der Photovoltaikanlage erkannt.");
        }

        if (solarmanService.getAlarm() != null) {
            newModel.getWarnings().add("Photovoltaikanlage meldet Fehler: " + solarmanService.getAlarm());
        }
    }

}
