package homecontroller.domain.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import homecontroller.dao.HistoryDatabaseDAO;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.Datapoint;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.FrontDoor;
import homecontroller.domain.model.Heating;
import homecontroller.domain.model.Hint;
import homecontroller.domain.model.HomematicConstants;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Intensity;
import homecontroller.domain.model.OutdoorClimate;
import homecontroller.domain.model.PowerMeter;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.ShutterPosition;
import homecontroller.domain.model.Switch;
import homecontroller.domain.model.Tendency;
import homecontroller.domain.model.Type;
import homecontroller.domain.model.ValueWithTendency;
import homecontroller.domain.model.Window;
import homecontroller.service.CameraService;
import homecontroller.service.HomematicAPI;
import homecontroller.service.PushService;
import homelibrary.dao.ModelObjectDAO;

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
	private static final BigDecimal TARGET_HUMIDITY_MAX_INSIDE = new BigDecimal("65");

	private static final BigDecimal SUN_INTENSITY_NO = new BigDecimal("3");
	private static final BigDecimal SUN_INTENSITY_LOW = new BigDecimal("8");
	private static final BigDecimal SUN_INTENSITY_MEDIUM = new BigDecimal("15");

	private static final long HINT_TIMEOUT_MINUTES_AFTER_BOOST = 90L;

	private static final Object REFRESH_MONITOR = new Object();
	private static final long REFRESH_TIMEOUT = 10L * 1000L; // 10 sec

	private static final String AUTOMATIC = "Automatic";

	@Autowired
	private HomematicAPI api;

	@Autowired
	private CameraService cameraService;

	@Autowired
	private PushService pushService;

	@Autowired
	private HistoryDatabaseDAO historyDAO;

	@PostConstruct
	public void init() {

		try {
			refreshHouseModel(false);
		} catch (Exception e) {
			LogFactory.getLog(HouseService.class).error("Could not initialize HouseService completly.", e);
		}
	}

	@Scheduled(fixedDelay = (1000 * 60))
	private void scheduledRefreshHouseModel() {
		refreshHouseModel(false);
	}

	public synchronized void refreshHouseModel(boolean notify) {

		HouseModel oldModel = ModelObjectDAO.getInstance().readHouseModel();

		HouseModel newModel = refreshModel();
		calculateConclusion(oldModel, newModel);
		ModelObjectDAO.getInstance().write(newModel);

		if (notify) {
			synchronized (REFRESH_MONITOR) {
				REFRESH_MONITOR.notifyAll();
			}
		}

		updateHomematicSystemVariables(oldModel, newModel);
		updateCameraPictures(oldModel, newModel);

		calculateHints(newModel);

		pushService.send(oldModel, newModel);
	}

	private HouseModel refreshModel() {

		api.refresh();

		HouseModel newModel = new HouseModel();

		newModel.setClimateBathRoom(readRoomClimate(Device.THERMOSTAT_BAD));
		newModel.setHeatingBathRoom(readHeating(Device.THERMOSTAT_BAD));
		newModel.setClimateKidsRoom(readRoomClimate(Device.THERMOMETER_KINDERZIMMER));
		newModel.setClimateLivingRoom(readRoomClimate(Device.THERMOMETER_WOHNZIMMER));
		newModel.setClimateBedRoom(readRoomClimate(Device.THERMOMETER_SCHLAFZIMMER));

		newModel.setLeftWindowBedRoom(readWindow(Device.ROLLLADE_SCHLAFZIMMER_LINKS));

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

		if (actual.getValue() == null) {
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

	public void calculateHints(HouseModel newModel) {

		lookupHint(newModel.getClimateKidsRoom(), null, newModel.getClimateEntrance());
		lookupHint(newModel.getClimateBathRoom(), newModel.getHeatingBathRoom(),
				newModel.getClimateEntrance());
		lookupHint(newModel.getClimateBedRoom(), null, newModel.getClimateTerrace());
		lookupHint(newModel.getClimateLivingRoom(), null, newModel.getClimateTerrace());
	}

	private void lookupHint(RoomClimate room, Heating heating, OutdoorClimate outdoor) {
		lookupTemperatureHint(room, heating, outdoor);
		lookupHumidityHint(room);
	}

	private void lookupHumidityHint(RoomClimate room) {

		if (room.getHumidity() == null) {
			return;
		}

		if (room.getHumidity().getValue().compareTo(TARGET_HUMIDITY_MAX_INSIDE) > 0) {
			room.getHints().add(Hint.DECREASE_HUMIDITY);
		} else if (room.getHumidity().getValue().compareTo(TARGET_HUMIDITY_MIN_INSIDE) < 0) {
			room.getHints().add(Hint.INCREASE_HUMIDITY);
		}
	}

	private void lookupTemperatureHint(RoomClimate room, Heating heating, OutdoorClimate outdoor) {

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
					room.getHints().add(Hint.TURN_ON_AIRCONDITION);
				} else {
					room.getHints().add(Hint.OPEN_WINDOW);
				}
			}
		} else if (room.getTemperature().getValue().compareTo(temperatureLimit) > 0
				&& outdoor.getSunBeamIntensity().ordinal() > Intensity.LOW.ordinal()) {
			room.getHints().add(Hint.CLOSE_ROLLER_SHUTTER);
		}
	}

	private boolean isHeatingIsCauseForHighRoomTemperature(Heating heating, BigDecimal temperatureLimit) {
		return heating != null && (heating.isBoostActive()
				|| heating.getTargetTemperature().compareTo(temperatureLimit) > 0
				|| historyDAO.minutesSinceLastHeatingBoost(heating) < HINT_TIMEOUT_MINUTES_AFTER_BOOST);
	}

	private boolean isTooColdOutsideSoNoNeedToCoolingDownRoom(BigDecimal roomTemperature) {

		if (ModelObjectDAO.getInstance().readHistoryModel() == null || ModelObjectDAO.getInstance()
				.readHistoryModel().getHighestOutsideTemperatureInLast24Hours() == null) {
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
		api.changeBooleanState(device.accessKeyXmlApi(Datapoint.STATE), value);
		refreshHouseModel(false);
	}

	public void toggleautomation(Device device, AutomationState value) {
		api.changeBooleanState(device.programNamePrefix() + AUTOMATIC, value.isBooleanValue());
		refreshHouseModel(false);
	}

	public synchronized void heatingBoost(Device device) throws InterruptedException {
		api.runProgram(device.programNamePrefix() + "Boost");
		synchronized (REFRESH_MONITOR) {
			// Just trying to wait for notification from CCU.
			// It's no big problem if this is the wrong notification.
			// We're only howing once the old value.
			REFRESH_MONITOR.wait(REFRESH_TIMEOUT); // NOSONAR
		}
	}

	// needs to be synchronized because of using ccu-systemwide temperature
	// variable
	public synchronized void heatingManual(Device device, BigDecimal temperature)
			throws InterruptedException {
		api.changeString(device.programNamePrefix() + "Temperature", temperature.toString());
		api.runProgram(device.programNamePrefix() + "Manual");
		synchronized (REFRESH_MONITOR) {
			// Just trying to wait for notification from CCU.
			// It's no big problem if this is the wrong notification.
			// We're only showing once the old value.
			REFRESH_MONITOR.wait(REFRESH_TIMEOUT); // NOSONAR
		}
	}

	private void updateHomematicSystemVariables(HouseModel oldModel, HouseModel newModel) {

		if (newModel.getConclusionClimateFacadeMin() != null
				&& newModel.getConclusionClimateFacadeMin().getTemperature().getValue() != null
				&& (oldModel == null
						|| oldModel.getConclusionClimateFacadeMin().getTemperature().getValue().compareTo(
								newModel.getConclusionClimateFacadeMin().getTemperature().getValue()) != 0)) {
			api.changeString(newModel.getConclusionClimateFacadeMin().getDevice().getType().getTypeName(),
					newModel.getConclusionClimateFacadeMin().getTemperature().getValue().toString());
		}

		if (doorbellTimestampChanged(oldModel, newModel)) {
			api.changeString(newModel.getFrontDoor().getDeviceDoorBellHistory().getType().getTypeName(),
					Long.toString(newModel.getFrontDoor().getTimestampLastDoorbell()));
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
		return doorbellOld != doorbellNew && doorbellNew > 0 && oldModel != null;
	}

	private OutdoorClimate readOutdoorClimate(Device outside, Device diff) {
		OutdoorClimate outdoorClimate = new OutdoorClimate();
		outdoorClimate.setTemperature(new ValueWithTendency<BigDecimal>(
				api.getAsBigDecimal(outside.accessKeyXmlApi(Datapoint.TEMPERATURE))));
		outdoorClimate.setSunBeamIntensity(
				lookupIntensity(api.getAsBigDecimal(diff.accessKeyXmlApi(Datapoint.TEMPERATURE))));
		outdoorClimate.setDevice(outside);

		return outdoorClimate;
	}

	private RoomClimate readRoomClimate(Device thermometer) {
		RoomClimate roomClimate = new RoomClimate();
		roomClimate.setTemperature(new ValueWithTendency<BigDecimal>(
				api.getAsBigDecimal(thermometer.accessKeyXmlApi(Datapoint.ACTUAL_TEMPERATURE))));
		BigDecimal humidity = api.getAsBigDecimal(thermometer.accessKeyXmlApi(Datapoint.HUMIDITY));
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
		heatingModel.setBoostActive(api.getAsBigDecimal(heating.accessKeyXmlApi(Datapoint.CONTROL_MODE))
				.compareTo(HomematicConstants.HEATING_CONTROL_MODE_BOOST) == 0);
		BigDecimal boostLeft = api.getAsBigDecimal(heating.accessKeyXmlApi(Datapoint.BOOST_STATE));
		heatingModel.setBoostMinutesLeft(boostLeft == null ? 0 : boostLeft.intValue());
		heatingModel.setTargetTemperature(
				api.getAsBigDecimal(heating.accessKeyXmlApi(Datapoint.SET_TEMPERATURE)));
		heatingModel.setDevice(heating);

		return heatingModel;
	}

	private Window readWindow(Device shutter) { // TODO: D_U_M_M_Y
		Window window = new Window();
		window.setDevice(shutter);
		window.setShutterPositionPercentage(30);
		window.setShutterPosition(ShutterPosition.fromPosition(window.getShutterPositionPercentage()));
		window.setShutterAutomation(true);
		window.setShutterAutomationInfoText("Dummy Text");
		return window;
	}

	private Switch readSwitchState(Device device) {
		Switch switchModel = new Switch();
		switchModel.setState(api.getAsBoolean(device.accessKeyXmlApi(Datapoint.STATE)));
		switchModel.setDevice(device);
		switchModel.setAutomation(api.getAsBoolean(device.programNamePrefix() + AUTOMATIC));
		switchModel
				.setAutomationInfoText(api.getAsString(device.programNamePrefix() + AUTOMATIC + "InfoText"));
		return switchModel;
	}

	private FrontDoor readFrontDoor() {

		FrontDoor frontDoor = new FrontDoor();
		frontDoor.setDeviceCamera(Device.HAUSTUER_KAMERA);
		frontDoor.setDeviceDoorBell(Device.HAUSTUER_KLINGEL);
		frontDoor.setDeviceDoorBellHistory(Device.HAUSTUER_KLINGEL_HISTORIE);

		Long tsDoorbell = api.getTimestamp(Device.HAUSTUER_KLINGEL.accessKeyXmlApi(Datapoint.PRESS_SHORT));
		if (tsDoorbell == null || tsDoorbell == 0) {
			tsDoorbell = Long
					.parseLong(api.getAsString(Device.HAUSTUER_KLINGEL_HISTORIE.getType().getTypeName()));
		}
		frontDoor.setTimestampLastDoorbell(tsDoorbell);

		return frontDoor;
	}

	private PowerMeter readPowerConsumption(Device device) {

		PowerMeter model = new PowerMeter();
		model.setDevice(device);
		model.setActualConsumption(new ValueWithTendency<BigDecimal>(
				api.getAsBigDecimal(device.accessKeyXmlApi(Datapoint.POWER))));
		return model;
	}

	private void checkLowBattery(HouseModel model, Device device) {

		boolean state = false;

		if (device.isHomematic()) {
			state = api.getAsBoolean(device.accessMainDeviceKeyXmlApi(Datapoint.LOWBAT));
		} else if (device.isHomematicIP()) {
			state = api.getAsBoolean(device.accessMainDeviceKeyXmlApi(Datapoint.LOW_BAT));
		}

		if (state) {
			model.getLowBatteryDevices().add(device.getDescription());
		}
	}
}
