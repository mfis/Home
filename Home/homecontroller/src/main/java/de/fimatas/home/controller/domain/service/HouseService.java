package de.fimatas.home.controller.domain.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.service.CameraService;
import de.fimatas.home.controller.service.HumidityCalculator;
import de.fimatas.home.controller.service.PushService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.AutomationState;
import de.fimatas.home.library.domain.model.Camera;
import de.fimatas.home.library.domain.model.Climate;
import de.fimatas.home.library.domain.model.Doorbell;
import de.fimatas.home.library.domain.model.Doorlock;
import de.fimatas.home.library.domain.model.Heating;
import de.fimatas.home.library.domain.model.Hint;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.Intensity;
import de.fimatas.home.library.domain.model.OutdoorClimate;
import de.fimatas.home.library.domain.model.PowerMeter;
import de.fimatas.home.library.domain.model.RoomClimate;
import de.fimatas.home.library.domain.model.Shutter;
import de.fimatas.home.library.domain.model.ShutterPosition;
import de.fimatas.home.library.domain.model.StateValue;
import de.fimatas.home.library.domain.model.Switch;
import de.fimatas.home.library.domain.model.Tendency;
import de.fimatas.home.library.domain.model.ValueWithTendency;
import de.fimatas.home.library.domain.model.WindowSensor;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.homematic.model.HomematicConstants;
import de.fimatas.home.library.homematic.model.Type;
import de.fimatas.home.library.model.Message;
import de.fimatas.home.library.util.HomeAppConstants;
import mfi.files.api.UserService;

@Component
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

    private static final String AUTOMATIC = "Automatic";

    private static final String EVENT = "Event";

    private static final String BUSY = "Busy";

    private static final String IS_OPENED = "IsOpened";

    private static final String TIMESTAMP = "Timestamp";

    private static final Log LOG = LogFactory.getLog(HouseService.class);

    @Autowired
    private HomematicAPI hmApi;

    @Autowired
    private HumidityCalculator humidityCalculator;

    @Autowired
    private CameraService cameraService;

    @Autowired
    private PushService pushService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private UserService userService;

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                refreshHouseModel();
            } catch (Exception e) {
                LOG.error("Could not initialize HouseService completly.", e);
            }
        });
    }

    @Scheduled(fixedDelay = (1000 * HomeAppConstants.MODEL_DEFAULT_INTERVAL_SECONDS))
    private void scheduledRefreshHouseModel() {
        refreshHouseModel();
    }

    public synchronized void refreshHouseModel() {

        HouseModel oldModel = ModelObjectDAO.getInstance().readHouseModel();
        HouseModel newModel = refreshModel(oldModel);
        if (newModel == null) {
            return;
        }

        historyService.saveNewValues();

        calculateConclusion(oldModel, newModel);
        ModelObjectDAO.getInstance().write(newModel);

        calculateHints(oldModel, newModel);

        pushService.sendAfterModelRefresh(oldModel, newModel); // async
        uploadService.upload(newModel);

        updateCameraPictures(oldModel, newModel); // async
        updateHomematicSystemVariables(oldModel, newModel);
        cameraService.cleanUp();
    }

    private HouseModel refreshModel(HouseModel oldModel) {

        if (!hmApi.refresh()) {
            return null;
        }

        HouseModel newModel = new HouseModel();

        newModel.setClimateBathRoom(readRoomClimate(Device.THERMOSTAT_BAD));
        newModel.setHeatingBathRoom(readHeating(Device.THERMOSTAT_BAD));
        newModel.setClimateKidsRoom(readRoomClimate(Device.THERMOMETER_KINDERZIMMER));
        newModel.setClimateLivingRoom(readRoomClimate(Device.THERMOMETER_WOHNZIMMER));
        newModel.setClimateBedRoom(readRoomClimate(Device.THERMOMETER_SCHLAFZIMMER));
        newModel.setClimateLaundry(readRoomClimate(Device.THERMOMETER_WASCHKUECHE));

        newModel.setGuestRoomWindowSensor(readWindowSensorState(Device.FENSTERSENSOR_GAESTEZIMMER));

        // newModel.setLeftWindowBedRoom(readWindow(Device.ROLLLADE_SCHLAFZIMMER_LINKS)); // NOSONAR

        newModel.setClimateTerrace(
            readOutdoorClimate(Device.DIFF_TEMPERATUR_TERRASSE_AUSSEN, Device.DIFF_TEMPERATUR_TERRASSE_DIFF));
        newModel.setClimateEntrance(
            readOutdoorClimate(Device.DIFF_TEMPERATUR_EINFAHRT_AUSSEN, Device.DIFF_TEMPERATUR_EINFAHRT_DIFF));
        newModel.setClimateGarden(readOutdoorClimate(Device.THERMOMETER_GARTEN, null));

        newModel.setKitchenWindowLightSwitch(readSwitchState(Device.SCHALTER_KUECHE_LICHT));
        newModel.setWallboxSwitch(readSwitchState(Device.SCHALTER_WALLBOX));
        newModel.setWorkshopVentilationSwitch(readSwitchState(Device.SCHALTER_WERKSTATT_LUEFTUNG));

        newModel.setFrontDoorBell(readFrontDoorBell());
        newModel.setFrontDoorCamera(readFrontDoorCamera());
        newModel.setFrontDoorLock(readFrontDoorLock(oldModel));

        newModel.setTotalElectricalPowerConsumption(readPowerConsumption(Device.STROMZAEHLER_GESAMT));
        newModel.setWallboxElectricalPowerConsumption(readPowerConsumption(Device.STROMZAEHLER_WALLBOX));

        for (Device device : Device.values()) {
            checkLowBattery(newModel, device);
        }

        ckeckWarnings(newModel);

        return newModel;
    }

    private void calculateConclusion(HouseModel oldModel, HouseModel newModel) {

        List<OutdoorClimate> outdoor = List.of(//
            newModel.getClimateEntrance(), newModel.getClimateTerrace(), newModel.getClimateGarden() //
        ).stream().filter(c -> !c.isUnreach()).collect(Collectors.toList());

        calculateOutdoorMinMax(newModel, outdoor);
        calculateOutdoorHumidity(newModel);
        calculateTendencies(oldModel, newModel);
        calculateHumidityComparison(newModel);
    }

    private void calculateOutdoorHumidity(HouseModel newModel) {

        if (newModel.getConclusionClimateFacadeMin() == null) {
            return;
        }

        if (newModel.getConclusionClimateFacadeMin().getHumidity() == null && newModel.getClimateGarden() != null
            && newModel.getClimateGarden().getHumidity() != null) {
            double absoluteHumidity =
                humidityCalculator.relToAbs(newModel.getClimateGarden().getTemperature().getValue().doubleValue(),
                    newModel.getClimateGarden().getHumidity().getValue().doubleValue());
            newModel.getConclusionClimateFacadeMin()
                .setHumidity(new ValueWithTendency<>(BigDecimal.valueOf(humidityCalculator.absToRel(
                    newModel.getConclusionClimateFacadeMin().getTemperature().getValue().doubleValue(), absoluteHumidity))));
        }
    }

    private void calculateOutdoorMinMax(HouseModel newModel, List<OutdoorClimate> outdoor) {

        Comparator<OutdoorClimate> comparator =
            Comparator.comparing(OutdoorClimate::getTemperature, (t1, t2) -> t1.getValue().compareTo(t2.getValue()));
        Optional<OutdoorClimate> minTemperature = outdoor.stream().min(comparator);
        Optional<OutdoorClimate> maxTemperature = outdoor.stream().max(comparator);

        // compensating absent difference temperature value
        if (minTemperature.isPresent()) {
            BigDecimal diffGarden = newModel.getClimateGarden().getTemperature().getValue()
                .subtract(minTemperature.get().getTemperature().getValue()).abs();
            newModel.getClimateGarden().setSunBeamIntensity(lookupIntensity(diffGarden));
            newModel.setConclusionClimateFacadeMin(SerializationUtils.clone(minTemperature.get()));
            newModel.getConclusionClimateFacadeMin().setDevice(Device.AUSSENTEMPERATUR);
            newModel.getConclusionClimateFacadeMin().setBase(minTemperature.get().getDevice());
            newModel.getConclusionClimateFacadeMin().setMaxSideSunHeating(newModel.getConclusionClimateFacadeMax());
        } else {
            var empty = new OutdoorClimate();
            empty.setDevice(Device.AUSSENTEMPERATUR);
            empty.setUnreach(true);
            newModel.setConclusionClimateFacadeMin(empty);
        }

        if (maxTemperature.isPresent()) {
            newModel.setConclusionClimateFacadeMax(SerializationUtils.clone(maxTemperature.get()));
            BigDecimal sunShadeDiff = newModel.getConclusionClimateFacadeMax().getTemperature().getValue()
                .subtract(newModel.getConclusionClimateFacadeMin().getTemperature().getValue()).abs();
            newModel.getConclusionClimateFacadeMax().setSunHeatingInContrastToShadeIntensity(lookupIntensity(sunShadeDiff));
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
            oldModel == null ? null : oldModel.getTotalElectricalPowerConsumption(),
            newModel.getTotalElectricalPowerConsumption());
        calculatePowerConsumptionTendencies(newModel.getDateTime(),
            oldModel == null ? null : oldModel.getWallboxElectricalPowerConsumption(),
            newModel.getWallboxElectricalPowerConsumption());
    }

    void calculateHumidityComparison(HouseModel newModel) {

        if (newModel.getClimateGarden() == null || newModel.getClimateGarden().isUnreach()
            || newModel.getClimateGarden().getHumidity() == null) {
            return;
        }

        double absoluteHumidityGarden =
            humidityCalculator.relToAbs(newModel.getClimateGarden().getTemperature().getValue().doubleValue(),
                newModel.getClimateGarden().getHumidity().getValue().doubleValue());

        Map<String, RoomClimate> places = newModel.lookupFields(RoomClimate.class);

        for (Entry<String, RoomClimate> entry : places.entrySet()) {
            RoomClimate roomClimate = entry.getValue();
            if (roomClimate.getHumidity() != null) {
                double absoluteHumidityRoom = humidityCalculator.relToAbs(roomClimate.getTemperature().getValue().doubleValue(),
                    roomClimate.getHumidity().getValue().doubleValue());
                roomClimate.setHumidityWetterThanOutdoor(absoluteHumidityRoom > absoluteHumidityGarden);
            }
        }

    }

    private void calculatePowerConsumptionTendencies(long newModelDateTime, PowerMeter oldModel, PowerMeter newModel) {

        if (newModel == null || newModel.isUnreach()) {
            return;
        }

        ValueWithTendency<BigDecimal> referencePower;
        if (oldModel == null) {
            referencePower = newModel.getActualConsumption();
            referencePower.setReferenceValue(referencePower.getValue());
        } else {
            referencePower = oldModel.getActualConsumption();
        }
        calculateTendency(newModelDateTime, referencePower, newModel.getActualConsumption(), POWER_TENDENCY_DIFF);
    }

    private void calculateClimateTendencies(HouseModel newModel, Climate climateNew, Climate climateOld) {

        if (climateNew == null) {
            return;
        }

        // Temperature
        if (climateNew.getTemperature() != null) {
            ValueWithTendency<BigDecimal> referenceTemperature;
            if (climateOld == null) {
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
            if (climateOld == null) {
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
            lookupHint(oldModel != null ? oldModel.getClimateKidsRoom() : null, newModel.getClimateKidsRoom(), null,
                newModel.getClimateEntrance(), newModel.getDateTime());
            lookupHint(oldModel != null ? oldModel.getClimateBathRoom() : null, newModel.getClimateBathRoom(),
                newModel.getHeatingBathRoom(), newModel.getClimateGarden(), newModel.getDateTime());
            lookupHint(oldModel != null ? oldModel.getClimateBedRoom() : null, newModel.getClimateBedRoom(), null,
                newModel.getClimateTerrace(), newModel.getDateTime());
            lookupHint(oldModel != null ? oldModel.getClimateLivingRoom() : null, newModel.getClimateLivingRoom(), null,
                newModel.getClimateTerrace(), newModel.getDateTime());
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
        if (outdoor != null) {
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
        } else if (emperatureOutsideColderThanInside(room, outdoor, temperatureLimit)) {
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

    private boolean emperatureOutsideColderThanInside(RoomClimate room, OutdoorClimate outdoor, BigDecimal temperatureLimit) {
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
            LogFactory.getLog(HouseService.class).info("HighestOutsideTemperatureInLast24Hours == null");
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

    public synchronized void heatingBoost(Device device) {
        runProgram(device, "Boost");
    }

    // needs to be synchronized because of using ccu-systemwide temperature
    // variable
    public synchronized void heatingManual(Device device, BigDecimal temperature) {
        hmApi.executeCommand(homematicCommandBuilder.write(device, "Temperature", temperature.toString()));
        runProgram(device, "Manual");
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

    private void updateCameraPictures(HouseModel oldModel, HouseModel newModel) {

        // FrontDoor
        if (doorbellTimestampChanged(oldModel, newModel) && newModel.getFrontDoorCamera() != null
            && newModel.getFrontDoorCamera().getDevice() != null) {
            cameraService.takeEventPicture(newModel.getFrontDoorBell(), newModel.getFrontDoorCamera());
        }
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

        if (diff != null) {
            outdoorClimate.setSunBeamIntensity(
                lookupIntensity(hmApi.getAsBigDecimal(homematicCommandBuilder.read(diff, Datapoint.TEMPERATURE))));
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

    private Heating readHeating(Device heating) {

        Heating heatingModel = new Heating();
        heatingModel.setDevice(heating);
        heatingModel.setUnreach(hmApi.isDeviceUnreachableOrNotSending(heating));
        if (heatingModel.isUnreach()) {
            return heatingModel;
        }

        heatingModel.setBoostActive(hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.CONTROL_MODE))
            .compareTo(HomematicConstants.HEATING_CONTROL_MODE_BOOST) == 0);
        BigDecimal boostLeft = hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.BOOST_STATE));
        heatingModel.setBoostMinutesLeft(boostLeft == null ? 0 : boostLeft.intValue());
        heatingModel
            .setTargetTemperature(hmApi.getAsBigDecimal(homematicCommandBuilder.read(heating, Datapoint.SET_TEMPERATURE)));
        heatingModel.setBusy(checkBusyState(heating));

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
        switchModel.setDevice(device);
        switchModel.setUnreach(hmApi.isDeviceUnreachableOrNotSending(device));
        if (switchModel.isUnreach()) {
            return switchModel;
        }

        switchModel.setState(hmApi.getAsBoolean(homematicCommandBuilder.read(device, Datapoint.STATE)));
        switchModel.setAutomation(hmApi.getAsBoolean(homematicCommandBuilder.read(device, AUTOMATIC)));
        switchModel.setAutomationInfoText(hmApi.getAsString(homematicCommandBuilder.read(device, AUTOMATIC + "InfoText")));
        return switchModel;
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

        frontDoor.setLockState(!hmApi.getAsBoolean(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, Datapoint.STATE))); // false=verriegelt
        frontDoor.setLockStateUncertain(
            hmApi.getAsBoolean(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, Datapoint.STATE_UNCERTAIN)));
        frontDoor.setOpen(hmApi.getAsBoolean(homematicCommandBuilder.read(Device.HAUSTUER_SCHLOSS, IS_OPENED)));

        boolean newBusy = checkBusyState(Device.HAUSTUER_SCHLOSS);
        if (oldModel != null && oldModel.getFrontDoorLock().isBusy() && !newBusy) {
            frontDoor.setBusy(doorLockHash(oldModel.getFrontDoorLock()) == doorLockHash(frontDoor));
        } else {
            frontDoor.setBusy(checkBusyState(Device.HAUSTUER_SCHLOSS));
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

        model.setActualConsumption(
            new ValueWithTendency<>(hmApi.getAsBigDecimal(homematicCommandBuilder.read(device, Datapoint.POWER))));
        return model;
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
    }

}
