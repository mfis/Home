package homecontroller.domain.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.FrontDoor;
import homecontroller.domain.model.Heating;
import homecontroller.domain.model.Hint;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Intensity;
import homecontroller.domain.model.OutdoorClimate;
import homecontroller.domain.model.PowerMeter;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.ShutterPosition;
import homecontroller.domain.model.Switch;
import homecontroller.domain.model.Tendency;
import homecontroller.domain.model.ValueWithTendency;
import homecontroller.domain.model.Window;
import homecontroller.service.CameraService;
import homecontroller.service.HomematicAPI;
import homecontroller.service.PushService;
import homelibrary.dao.ModelObjectDAO;
import homelibrary.homematic.model.Datapoint;
import homelibrary.homematic.model.Device;
import homelibrary.homematic.model.HomematicCommand;
import homelibrary.homematic.model.HomematicConstants;
import homelibrary.homematic.model.Type;

@Component
public class HouseService {

	private static final BigDecimal TARGET_TEMPERATURE_INSIDE = new BigDecimal("21");
	private static final BigDecimal TARGET_TEMPERATURE_TOLERANCE_OFFSET = new BigDecimal("1");
	private static final BigDecimal TEMPERATURE_DIFFERENCE_INSIDE_OUTSIDE_NO_ROOM_COOLDOWN_NEEDED = new BigDecimal(
			"6");

	private static final BigDecimal TEMPERATURE_TENDENCY_DIFF = new BigDecimal("0.199");
	private static final BigDecimal HUMIDITY_TENDENCY_DIFF = new BigDecimal("1.99");
	private static final BigDecimal POWER_TENDENCY_DIFF = new BigDecimal("99.99");

	private static final BigDecimal TARGET_HUMIDITY_MIN_INSIDE = new BigDecimal("40");
	private static final BigDecimal TARGET_HUMIDITY_MAX_INSIDE = new BigDecimal("70");

	private static final BigDecimal SUN_INTENSITY_NO = new BigDecimal("3");
	private static final BigDecimal SUN_INTENSITY_LOW = new BigDecimal("8");
	private static final BigDecimal SUN_INTENSITY_MEDIUM = new BigDecimal("15");

	private static final String AUTOMATIC = "Automatic";
	private static final String BUSY = "Busy";

	private static final Log LOG = LogFactory.getLog(HouseService.class);

	@Autowired
	private HomematicAPI api;

	@Autowired
	private CameraService cameraService;

	@Autowired
	private PushService pushService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private UploadService uploadService;

	@PostConstruct
	public void init() {
		CompletableFuture.runAsync(() -> {
			try {
				if (ModelObjectDAO.getInstance().readHistoryModel() == null) {
					historyService.refreshHistoryModelComplete();
				}
				refreshHouseModel();
			} catch (Exception e) {
				LOG.error("Could not initialize HouseService completly.", e);
			}
		});
	}

	@Scheduled(fixedDelay = (1000 * 5))
	private void scheduledRefreshHouseModel() {
		refreshHouseModel();
	}

	public synchronized void refreshHouseModel() {

		HouseModel oldModel = ModelObjectDAO.getInstance().readHouseModel();
		HouseModel newModel = refreshModel();
		if (newModel == null) {
			return;
		}

		historyService.saveNewValues();

		calculateConclusion(oldModel, newModel);
		ModelObjectDAO.getInstance().write(newModel);

		calculateHints(oldModel, newModel);

		pushService.send(oldModel, newModel); // async
		uploadService.upload(newModel);

		updateCameraPictures(oldModel, newModel); // async
		updateHomematicSystemVariables(oldModel, newModel);
		cameraService.cleanUp();
	}

	private HouseModel refreshModel() {

		if (!api.refresh()) {
			return null;
		}

		HouseModel newModel = new HouseModel();

		newModel.setClimateBathRoom(readRoomClimate(Device.THERMOSTAT_BAD));
		newModel.setHeatingBathRoom(readHeating(Device.THERMOSTAT_BAD));
		newModel.setClimateKidsRoom(readRoomClimate(Device.THERMOMETER_KINDERZIMMER));
		newModel.setClimateLivingRoom(readRoomClimate(Device.THERMOMETER_WOHNZIMMER));
		newModel.setClimateBedRoom(readRoomClimate(Device.THERMOMETER_SCHLAFZIMMER));
		newModel.setClimateLaundry(readRoomClimate(Device.THERMOMETER_WASCHKUECHE));

		// newModel.setLeftWindowBedRoom(readWindow(Device.ROLLLADE_SCHLAFZIMMER_LINKS));

		newModel.setClimateTerrace(readOutdoorClimate(Device.DIFF_TEMPERATUR_TERRASSE_AUSSEN,
				Device.DIFF_TEMPERATUR_TERRASSE_DIFF));
		newModel.setClimateEntrance(readOutdoorClimate(Device.DIFF_TEMPERATUR_EINFAHRT_AUSSEN,
				Device.DIFF_TEMPERATUR_EINFAHRT_DIFF));

		newModel.setKitchenWindowLightSwitch(readSwitchState(Device.SCHALTER_KUECHE_LICHT));

		newModel.setFrontDoor(readFrontDoor());

		newModel.setElectricalPowerConsumption(readPowerConsumption(Device.STROMZAEHLER));

		for (Device device : Device.values()) {
			checkLowBattery(newModel, device);
		}

		return newModel;
	}

	public void calculateConclusion(HouseModel oldModel, HouseModel newModel) {

		if (newModel.getClimateTerrace().getTemperature().getValue() == null
				|| newModel.getClimateEntrance().getTemperature().getValue() == null) {
			newModel.setConclusionClimateFacadeMin(null);
			newModel.setConclusionClimateFacadeMax(null);
			return;
		}

		if (newModel.getClimateTerrace().getTemperature().getValue()
				.compareTo(newModel.getClimateEntrance().getTemperature().getValue()) < 0) {
			newModel.setConclusionClimateFacadeMin(newModel.getClimateTerrace());
			newModel.setConclusionClimateFacadeMax(newModel.getClimateEntrance());
		} else {
			newModel.setConclusionClimateFacadeMin(newModel.getClimateEntrance());
			newModel.setConclusionClimateFacadeMax(newModel.getClimateTerrace());
		}

		BigDecimal sunShadeDiff = newModel.getConclusionClimateFacadeMax().getTemperature().getValue()
				.subtract(newModel.getConclusionClimateFacadeMin().getTemperature().getValue()).abs();
		newModel.getConclusionClimateFacadeMax()
				.setSunHeatingInContrastToShadeIntensity(lookupIntensity(sunShadeDiff));

		newModel.getConclusionClimateFacadeMin().setDevice(Device.AUSSENTEMPERATUR);
		newModel.getConclusionClimateFacadeMin()
				.setMaxSideSunHeating(newModel.getConclusionClimateFacadeMax());

		calculateTendencies(oldModel, newModel);
	}

	void calculateTendencies(HouseModel oldModel, HouseModel newModel) {

		Map<String, Climate> places = newModel.lookupFields(Climate.class);

		// Climate
		for (Entry<String, Climate> entry : places.entrySet()) {
			Climate climateNew = entry.getValue();
			Climate climateOld = oldModel != null ? oldModel.lookupField(entry.getKey(), Climate.class)
					: null;
			calculateClimateTendencies(newModel, climateNew, climateOld);
		}

		// Power consumption
		calculatePowerConsumptionTendencies(oldModel, newModel);
	}

	private void calculatePowerConsumptionTendencies(HouseModel oldModel, HouseModel newModel) {

		if (newModel.getElectricalPowerConsumption() != null) {
			ValueWithTendency<BigDecimal> referencePower;
			if (oldModel == null) {
				referencePower = newModel.getElectricalPowerConsumption().getActualConsumption();
				referencePower.setReferenceValue(referencePower.getValue());
			} else {
				referencePower = oldModel.getElectricalPowerConsumption().getActualConsumption();
			}
			calculateTendency(newModel, referencePower,
					newModel.getElectricalPowerConsumption().getActualConsumption(), POWER_TENDENCY_DIFF);
		}
	}

	private void calculateClimateTendencies(HouseModel newModel, Climate climateNew, Climate climateOld) {

		// Temperature
		if (climateNew.getTemperature() != null) {
			ValueWithTendency<BigDecimal> referenceTemperature;
			if (climateOld == null) {
				referenceTemperature = climateNew.getTemperature();
				referenceTemperature.setReferenceValue(referenceTemperature.getValue());
			} else {
				referenceTemperature = climateOld.getTemperature();
			}
			calculateTendency(newModel, referenceTemperature, climateNew.getTemperature(),
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
			calculateTendency(newModel, referenceHumidity, climateNew.getHumidity(), HUMIDITY_TENDENCY_DIFF);
		}
	}

	private void calculateTendency(HouseModel newModel, ValueWithTendency<BigDecimal> reference,
			ValueWithTendency<BigDecimal> actual, BigDecimal diffValue) {

		if (actual.getValue() == null || reference == null || reference.getReferenceValue() == null) {
			actual.setTendency(Tendency.NONE);
			return;
		}

		BigDecimal diff = actual.getValue().subtract(reference.getReferenceValue());

		if (diff.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(diffValue) > 0) {
			actual.setTendency(Tendency.RISE);
			actual.setReferenceValue(actual.getValue());
			actual.setReferenceDateTime(newModel.getDateTime());
		} else if (diff.compareTo(BigDecimal.ZERO) < 0 && diff.abs().compareTo(diffValue) > 0) {
			actual.setTendency(Tendency.FALL);
			actual.setReferenceValue(actual.getValue());
			actual.setReferenceDateTime(newModel.getDateTime());
		} else {
			long timeDiff = newModel.getDateTime() - reference.getReferenceDateTime();
			actual.setTendency(Tendency.calculate(reference, timeDiff));
			actual.setReferenceValue(reference.getReferenceValue());
			actual.setReferenceDateTime(reference.getReferenceDateTime());
		}
	}

	public void calculateHints(HouseModel oldModel, HouseModel newModel) {

		try {
			lookupHint(oldModel != null ? oldModel.getClimateKidsRoom() : null, newModel.getClimateKidsRoom(),
					null, newModel.getClimateEntrance(), newModel.getDateTime());
			lookupHint(oldModel != null ? oldModel.getClimateBathRoom() : null, newModel.getClimateBathRoom(),
					newModel.getHeatingBathRoom(), newModel.getClimateEntrance(), newModel.getDateTime());
			lookupHint(oldModel != null ? oldModel.getClimateBedRoom() : null, newModel.getClimateBedRoom(),
					null, newModel.getClimateTerrace(), newModel.getDateTime());
			lookupHint(oldModel != null ? oldModel.getClimateLivingRoom() : null,
					newModel.getClimateLivingRoom(), null, newModel.getClimateTerrace(),
					newModel.getDateTime());
			lookupHint(oldModel != null ? oldModel.getClimateLaundry() : null, newModel.getClimateLaundry(),
					null, null, newModel.getDateTime());
		} catch (RuntimeException re) {
			LogFactory.getLog(HouseService.class).error("Could not calculate hints:", re);
		}
	}

	private void lookupHint(RoomClimate old, RoomClimate room, Heating heating, OutdoorClimate outdoor,
			long dateTime) {
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

		if (room.getHumidity().getValue().compareTo(TARGET_HUMIDITY_MAX_INSIDE) > 0) {
			room.getHints().giveHint(Hint.DECREASE_HUMIDITY, dateTime);
		} else if (room.getHumidity().getValue().compareTo(TARGET_HUMIDITY_MIN_INSIDE) < 0) {
			room.getHints().giveHint(Hint.INCREASE_HUMIDITY, dateTime);
		}
	}

	private void lookupTemperatureHint(RoomClimate room, Heating heating, OutdoorClimate outdoor,
			long dateTime) {

		BigDecimal targetTemperature = heating != null ? heating.getTargetTemperature()
				: TARGET_TEMPERATURE_INSIDE;
		BigDecimal temperatureLimit = targetTemperature.add(TARGET_TEMPERATURE_TOLERANCE_OFFSET);

		if (room.getTemperature() == null || room.getTemperature().getValue() == null) {
			// nothing to do
		} else if (room.getTemperature().getValue().compareTo(temperatureLimit) < 0) {
			// TODO: using sun heating in the winter for warming up rooms
		} else if (isTooColdOutsideSoNoNeedToCoolingDownRoom(room.getTemperature().getValue())) {
			// no hint
		} else if (room.getTemperature().getValue().compareTo(temperatureLimit) > 0
				&& outdoor.getTemperature().getValue().compareTo(room.getTemperature().getValue()) < 0
				&& outdoor.getSunBeamIntensity().ordinal() <= Intensity.LOW.ordinal()) {
			if (isHeatingIsCauseForHighRoomTemperature(heating, temperatureLimit)) {
				// no hint
			} else {
				if (room.getDevice().getPlace().isAirCondition()) {
					room.getHints().giveHint(Hint.TURN_ON_AIRCONDITION, dateTime);
				} else {
					room.getHints().giveHint(Hint.OPEN_WINDOW, dateTime);
				}
			}
		} else if (room.getTemperature().getValue().compareTo(temperatureLimit) > 0
				&& outdoor.getSunBeamIntensity().ordinal() > Intensity.LOW.ordinal()) {
			room.getHints().giveHint(Hint.CLOSE_ROLLER_SHUTTER, dateTime);
		}
	}

	private boolean isHeatingIsCauseForHighRoomTemperature(Heating heating, BigDecimal temperatureLimit) {
		return heating != null && (heating.isBoostActive()
				|| heating.getTargetTemperature().compareTo(temperatureLimit) > 0);
	}

	private boolean isTooColdOutsideSoNoNeedToCoolingDownRoom(BigDecimal roomTemperature) {

		if (ModelObjectDAO.getInstance().readHistoryModel() == null || ModelObjectDAO.getInstance()
				.readHistoryModel().getHighestOutsideTemperatureInLast24Hours() == null) {
			LogFactory.getLog(HouseService.class).info("HighestOutsideTemperatureInLast24Hours == null");
			return true;
		}

		BigDecimal roomMinusOutside = roomTemperature.subtract(
				ModelObjectDAO.getInstance().readHistoryModel().getHighestOutsideTemperatureInLast24Hours());
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
		api.executeCommand(HomematicCommand.write(device, Datapoint.STATE, value));
	}

	public void toggleautomation(Device device, AutomationState value) {
		api.executeCommand(HomematicCommand.write(device, AUTOMATIC, value.isBooleanValue()));
	}

	public synchronized void heatingBoost(Device device) {
		runProgram(device, "Boost");
	}

	// needs to be synchronized because of using ccu-systemwide temperature
	// variable
	public synchronized void heatingManual(Device device, BigDecimal temperature) {
		api.executeCommand(HomematicCommand.write(device, "Temperature", temperature.toString()));
		runProgram(device, "Manual");
	}

	private void runProgram(Device device, String programSuffix) {
		api.executeCommand(HomematicCommand.write(device, BUSY, String.valueOf(System.currentTimeMillis())),
				HomematicCommand.exec(device, programSuffix));
	}

	private void updateHomematicSystemVariables(HouseModel oldModel, HouseModel newModel) {

		if (newModel.getConclusionClimateFacadeMin() != null
				&& newModel.getConclusionClimateFacadeMin().getTemperature().getValue() != null
				&& (oldModel == null
						|| oldModel.getConclusionClimateFacadeMin().getTemperature().getValue().compareTo(
								newModel.getConclusionClimateFacadeMin().getTemperature().getValue()) != 0)) {
			api.executeCommand(HomematicCommand.write(newModel.getConclusionClimateFacadeMin().getDevice(),
					Datapoint.SYSVAR_DUMMY,
					newModel.getConclusionClimateFacadeMin().getTemperature().getValue().toString()));
		}

		if (doorbellTimestampChanged(oldModel, newModel)) {
			api.executeCommand(HomematicCommand.write(newModel.getFrontDoor().getDeviceDoorBellHistory(),
					Datapoint.SYSVAR_DUMMY,
					Long.toString(newModel.getFrontDoor().getTimestampLastDoorbell())));
		}
	}

	private void updateCameraPictures(HouseModel oldModel, HouseModel newModel) {

		// FrontDoor
		if (doorbellTimestampChanged(oldModel, newModel)) {
			cameraService.takeEventPicture(newModel.getFrontDoor());
		}
	}

	public static boolean doorbellTimestampChanged(HouseModel oldModel, HouseModel newModel) {

		long doorbellOld = oldModel != null && oldModel.getFrontDoor() != null
				&& oldModel.getFrontDoor().getTimestampLastDoorbell() != null
						? oldModel.getFrontDoor().getTimestampLastDoorbell()
						: 0;
		long doorbellNew = newModel != null && newModel.getFrontDoor() != null
				&& newModel.getFrontDoor().getTimestampLastDoorbell() != null
						? newModel.getFrontDoor().getTimestampLastDoorbell()
						: 0;

		long minutesAgo = (System.currentTimeMillis() - doorbellNew) / 1000 / 60;

		return doorbellOld != doorbellNew && doorbellNew > 0 && oldModel != null && minutesAgo < 10;
	}

	private OutdoorClimate readOutdoorClimate(Device outside, Device diff) {
		OutdoorClimate outdoorClimate = new OutdoorClimate();
		outdoorClimate.setTemperature(new ValueWithTendency<BigDecimal>(
				api.getAsBigDecimal(HomematicCommand.read(outside, Datapoint.TEMPERATURE))));
		outdoorClimate.setSunBeamIntensity(
				lookupIntensity(api.getAsBigDecimal(HomematicCommand.read(diff, Datapoint.TEMPERATURE))));
		outdoorClimate.setDevice(outside);

		return outdoorClimate;
	}

	private RoomClimate readRoomClimate(Device thermometer) {
		RoomClimate roomClimate = new RoomClimate();
		roomClimate.setTemperature(new ValueWithTendency<BigDecimal>(
				api.getAsBigDecimal(HomematicCommand.read(thermometer, Datapoint.ACTUAL_TEMPERATURE))));
		BigDecimal humidity = thermometer.getType() == Type.THERMOSTAT ? null
				: api.getAsBigDecimal(HomematicCommand.read(thermometer, Datapoint.HUMIDITY));
		if (humidity != null) {
			roomClimate.setHumidity(new ValueWithTendency<BigDecimal>(humidity));
		}
		roomClimate.setDevice(thermometer);
		if (thermometer.getType() != Type.THERMOMETER) {
			roomClimate.setSubType(Type.THERMOMETER);
		}
		return roomClimate;
	}

	private Heating readHeating(Device heating) {
		Heating heatingModel = new Heating();
		heatingModel
				.setBoostActive(api.getAsBigDecimal(HomematicCommand.read(heating, Datapoint.CONTROL_MODE))
						.compareTo(HomematicConstants.HEATING_CONTROL_MODE_BOOST) == 0);
		BigDecimal boostLeft = api.getAsBigDecimal(HomematicCommand.read(heating, Datapoint.BOOST_STATE));
		heatingModel.setBoostMinutesLeft(boostLeft == null ? 0 : boostLeft.intValue());
		heatingModel.setTargetTemperature(
				api.getAsBigDecimal(HomematicCommand.read(heating, Datapoint.SET_TEMPERATURE)));
		heatingModel.setDevice(heating);
		heatingModel.setBusy(checkBusyState(heating));

		return heatingModel;
	}

	private boolean checkBusyState(Device device) {

		String busyString = api.getAsString(HomematicCommand.read(device, BUSY));
		if (StringUtils.isNotBlank(busyString) && StringUtils.isNumeric(busyString)) {
			long busyTimestamp = Long.parseLong(busyString);
			long diff = System.currentTimeMillis() - busyTimestamp;
			if (diff < 1000 * 60 * 3) {
				return true;
			}
			api.executeCommand(HomematicCommand.write(device, BUSY, StringUtils.EMPTY));
		}
		return false;
	}

	@SuppressWarnings("unused")
	private Window readWindow(Device shutter) { // TODO: D_U_M_M_Y
		Window window = new Window();
		window.setDevice(shutter);
		window.setShutterPositionPercentage(30);
		window.setShutterPosition(ShutterPosition.fromPosition(window.getShutterPositionPercentage()));
		window.setShutterAutomation(true);
		window.setShutterAutomationInfoText("Dummy Text");
		return window;
	}

	public void shutterPosition(Device device, int parseInt) {
		// TODO: D U M M Y
	}

	private Switch readSwitchState(Device device) {
		Switch switchModel = new Switch();
		switchModel.setState(api.getAsBoolean(HomematicCommand.read(device, Datapoint.STATE)));
		switchModel.setDevice(device);
		switchModel.setAutomation(api.getAsBoolean(HomematicCommand.read(device, AUTOMATIC)));
		switchModel.setAutomationInfoText(
				api.getAsString(HomematicCommand.read(device, AUTOMATIC + "InfoText")));
		return switchModel;
	}

	private FrontDoor readFrontDoor() {

		FrontDoor frontDoor = new FrontDoor();
		frontDoor.setDeviceCamera(Device.HAUSTUER_KAMERA);
		frontDoor.setDeviceDoorBell(Device.HAUSTUER_KLINGEL);
		frontDoor.setDeviceDoorBellHistory(Device.HAUSTUER_KLINGEL_HISTORIE);

		Long tsDoorbell = api
				.getTimestamp(HomematicCommand.readTS(Device.HAUSTUER_KLINGEL, Datapoint.PRESS_SHORT));
		if (tsDoorbell == null || tsDoorbell == 0) {
			tsDoorbell = Long.parseLong(api
					.getAsString(HomematicCommand.read(Device.HAUSTUER_KLINGEL_HISTORIE, StringUtils.EMPTY)));
		}
		frontDoor.setTimestampLastDoorbell(tsDoorbell);

		return frontDoor;
	}

	private PowerMeter readPowerConsumption(Device device) {

		PowerMeter model = new PowerMeter();
		model.setDevice(device);
		model.setActualConsumption(new ValueWithTendency<BigDecimal>(
				api.getAsBigDecimal(HomematicCommand.read(device, Datapoint.POWER))));
		return model;
	}

	private void checkLowBattery(HouseModel model, Device device) {

		boolean state = api.getAsBoolean(HomematicCommand.read(device, device.lowBatDatapoint()));
		if (state) {
			model.getLowBatteryDevices().add(device.getDescription());
		}
	}

}
