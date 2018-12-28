package homecontroller.domain.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import homecontroller.dao.ModelDAO;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.Datapoint;
import homecontroller.domain.model.Device;
import homecontroller.domain.model.HeatingModel;
import homecontroller.domain.model.Hint;
import homecontroller.domain.model.HomematicConstants;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Intensity;
import homecontroller.domain.model.OutdoorClimate;
import homecontroller.domain.model.PowerMeterModel;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.ShutterPosition;
import homecontroller.domain.model.SwitchModel;
import homecontroller.domain.model.Tendency;
import homecontroller.domain.model.ValueWithTendency;
import homecontroller.domain.model.Window;
import homecontroller.service.HomematicAPI;
import homecontroller.service.PushService;

@Component
public class HouseService {

	private static final BigDecimal TARGET_TEMPERATURE_INSIDE = new BigDecimal("21");
	private static final BigDecimal TARGET_TEMPERATURE_TOLERANCE_OFFSET = new BigDecimal("1");
	private static final BigDecimal TEMPERATURE_DIFFERENCE_INSIDE_OUTSIDE_NO_ROOM_COOLDOWN_NEEDED = new BigDecimal(
			"6");

	private static final BigDecimal TEMPERATURE_TENDENCY_DIFF = new BigDecimal("0.199");
	private static final BigDecimal HUMIDITY_TENDENCY_DIFF = new BigDecimal("1.99");
	private static final BigDecimal POWER_TENDENCY_DIFF = new BigDecimal("99.99");

	private static final BigDecimal TARGET_HUMIDITY_MIN_INSIDE = new BigDecimal("45");
	private static final BigDecimal TARGET_HUMIDITY_MAX_INSIDE = new BigDecimal("65");

	private static final BigDecimal SUN_INTENSITY_NO = new BigDecimal("3");
	private static final BigDecimal SUN_INTENSITY_LOW = new BigDecimal("8");
	private static final BigDecimal SUN_INTENSITY_MEDIUM = new BigDecimal("15");

	private static final long HINT_TIMEOUT_MINUTES_AFTER_BOOST = 90L;

	private static final Object REFRESH_MONITOR = new Object();
	private static final long REFRESH_TIMEOUT = 5L * 1000L; // 5 sec

	@Autowired
	private HomematicAPI api;

	@Autowired
	private PushService pushService;

	@Autowired
	private HistoryDAO historyDAO;

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

	public void refreshHouseModel(boolean notify) {

		HouseModel oldModel = ModelDAO.getInstance().readHouseModel();

		HouseModel newModel = refreshModel();
		calculateConclusion(oldModel, newModel);
		ModelDAO.getInstance().write(newModel);

		if (notify) {
			synchronized (REFRESH_MONITOR) {
				REFRESH_MONITOR.notifyAll();
			}
		}

		updateHomematicSystemVariables(oldModel, newModel);

		calculateHints(newModel);
		pushService.send(oldModel, newModel);
	}

	private HouseModel refreshModel() {

		api.refresh();

		HouseModel newModel = new HouseModel();

		newModel.setClimateBathRoom(readRoomClimate(Device.THERMOSTAT_BAD, Device.THERMOSTAT_BAD));
		newModel.setClimateKidsRoom(readRoomClimate(Device.THERMOMETER_KINDERZIMMER));
		newModel.setClimateLivingRoom(readRoomClimate(Device.THERMOMETER_WOHNZIMMER));
		newModel.setClimateBedRoom(readRoomClimate(Device.THERMOMETER_SCHLAFZIMMER));

		newModel.setLeftWindowBedRoom(readWindow(Device.ROLLLADE_SCHLAFZIMMER_LINKS));

		newModel.setClimateTerrace(readOutdoorClimate(Device.DIFFERENZTEMPERATUR_TERRASSE_AUSSEN,
				Device.DIFFERENZTEMPERATUR_TERRASSE_DIFF));
		newModel.setClimateEntrance(readOutdoorClimate(Device.DIFFERENZTEMPERATUR_EINFAHRT_AUSSEN,
				Device.DIFFERENZTEMPERATUR_EINFAHRT_DIFF));

		newModel.setKitchenWindowLightSwitch(readSwitchState(Device.SCHALTER_KUECHE_LICHT));

		newModel.setElectricalPowerConsumption(readPowerConsumption(Device.STROMZAEHLER));

		for (Device device : Device.values()) {
			checkLowBattery(newModel, device);
		}

		return newModel;
	}

	public void calculateConclusion(HouseModel oldModel, HouseModel newModel) {

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

		calculateTendencies(oldModel, newModel);
	}

	void calculateTendencies(HouseModel oldModel, HouseModel newModel) {

		Map<String, Climate> places = newModel.lookupFields(Climate.class);

		for (Entry<String, Climate> entry : places.entrySet()) {

			Climate climateNew = entry.getValue();
			Climate climateOld = oldModel != null ? oldModel.lookupField(entry.getKey(), Climate.class)
					: null;

			// Temperature
			ValueWithTendency<BigDecimal> referenceTemperature;
			if (climateOld == null) {
				referenceTemperature = climateNew.getTemperature();
				referenceTemperature.setReferenceValue(referenceTemperature.getValue());
			} else {
				referenceTemperature = climateOld.getTemperature();
			}
			calculateTendency(newModel, referenceTemperature, climateNew.getTemperature(),
					TEMPERATURE_TENDENCY_DIFF);

			// Humidity
			if (climateNew.getHumidity() != null) {
				ValueWithTendency<BigDecimal> referenceHumidity;
				if (climateOld == null) {
					referenceHumidity = climateNew.getHumidity();
					referenceHumidity.setReferenceValue(referenceHumidity.getValue());
				} else {
					referenceHumidity = climateOld.getHumidity();
				}
				calculateTendency(newModel, referenceHumidity, climateNew.getHumidity(),
						HUMIDITY_TENDENCY_DIFF);
			}
		}

		// Power consumption
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

	private void calculateTendency(HouseModel newModel, ValueWithTendency<BigDecimal> reference,
			ValueWithTendency<BigDecimal> actual, BigDecimal diffValue) {

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

		lookupHint(newModel.getClimateKidsRoom(), newModel.getClimateEntrance());
		lookupHint(newModel.getClimateBathRoom(), newModel.getClimateEntrance());
		lookupHint(newModel.getClimateBedRoom(), newModel.getClimateTerrace());
		lookupHint(newModel.getClimateLivingRoom(), newModel.getClimateTerrace());
	}

	private void lookupHint(RoomClimate room, OutdoorClimate outdoor) {
		lookupTemperatureHint(room, outdoor);
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

	private void lookupTemperatureHint(RoomClimate room, OutdoorClimate outdoor) {

		BigDecimal targetTemperature = room.getHeating() != null ? room.getHeating().getTargetTemperature()
				: TARGET_TEMPERATURE_INSIDE;
		BigDecimal temperatureLimit = targetTemperature.add(TARGET_TEMPERATURE_TOLERANCE_OFFSET);

		if (room.getTemperature() == null) {
			// nothing to do
		} else if (room.getTemperature().getValue().compareTo(temperatureLimit) < 0) {
			// TODO: using sun heating in the winter for warming up rooms
		} else if (isTooColdOutsideSoNoNeedToCoolingDownRoom(room.getTemperature().getValue())) {
			// no hint
		} else if (room.getTemperature().getValue().compareTo(temperatureLimit) > 0
				&& outdoor.getTemperature().getValue().compareTo(room.getTemperature().getValue()) < 0
				&& outdoor.getSunBeamIntensity().ordinal() <= Intensity.LOW.ordinal()) {
			if (isHeatingIsCauseForHighRoomTemperature(room, temperatureLimit)) {
				// no hint
			} else {
				room.getHints().add(Hint.OPEN_WINDOW);
			}
		} else if (room.getTemperature().getValue().compareTo(temperatureLimit) > 0
				&& outdoor.getSunBeamIntensity().ordinal() > Intensity.LOW.ordinal()) {
			room.getHints().add(Hint.CLOSE_ROLLER_SHUTTER);
		}
	}

	private boolean isHeatingIsCauseForHighRoomTemperature(RoomClimate room, BigDecimal temperatureLimit) {
		return room.getHeating() != null && (room.getHeating().isBoostActive()
				|| room.getHeating().getTargetTemperature().compareTo(temperatureLimit) > 0
				|| historyDAO.minutesSinceLastHeatingBoost(room) < HINT_TIMEOUT_MINUTES_AFTER_BOOST);
	}

	private boolean isTooColdOutsideSoNoNeedToCoolingDownRoom(BigDecimal roomTemperature) {

		if (ModelDAO.getInstance().readHistoryModel().getHighestOutsideTemperatureInLast24Hours() == null) {
			return true;
		}

		BigDecimal roomMinusOutside = roomTemperature.subtract(
				ModelDAO.getInstance().readHistoryModel().getHighestOutsideTemperatureInLast24Hours());
		return roomMinusOutside.compareTo(TEMPERATURE_DIFFERENCE_INSIDE_OUTSIDE_NO_ROOM_COOLDOWN_NEEDED) > 0;
	}

	private Intensity lookupIntensity(BigDecimal value) {
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

	public void toggle(String devIdVar) {
		api.toggleBooleanState(devIdVar);
		refreshHouseModel(false);
	}

	public synchronized void heatingBoost(String prefix) throws InterruptedException {
		api.runProgram(prefix + "Boost");
		synchronized (REFRESH_MONITOR) {
			// Just trying to wait for notification from CCU.
			// It's no big problem if this is the wrong notification.
			// We're only showing once the old value.
			REFRESH_MONITOR.wait(REFRESH_TIMEOUT); // NOSONAR
		}
	}

	// needs to be synchronized because of using ccu-systemwide temperature
	// variable
	public synchronized void heatingManual(String prefix, String temperature) throws InterruptedException {
		temperature = StringUtils.replace(temperature, ",", "."); // decimalpoint
		api.changeValue(prefix + "Temperature", temperature);
		api.runProgram(prefix + "Manual");
		synchronized (REFRESH_MONITOR) {
			// Just trying to wait for notification from CCU.
			// It's no big problem if this is the wrong notification.
			// We're only showing once the old value.
			REFRESH_MONITOR.wait(REFRESH_TIMEOUT); // NOSONAR
		}
	}

	private void updateHomematicSystemVariables(HouseModel oldModel, HouseModel newModel) {

		if (oldModel == null || oldModel.getConclusionClimateFacadeMin().getTemperature().getValue()
				.compareTo(newModel.getConclusionClimateFacadeMin().getTemperature().getValue()) != 0) {
			api.changeValue(Device.AUSSENTEMPERATUR.getType(),
					newModel.getConclusionClimateFacadeMin().getTemperature().toString());
		}
	}

	private OutdoorClimate readOutdoorClimate(Device outside, Device diff) {
		OutdoorClimate outdoorClimate = new OutdoorClimate();
		outdoorClimate.setTemperature(new ValueWithTendency<BigDecimal>(
				api.getAsBigDecimal(outside.accessKeyXmlApi(Datapoint.TEMPERATURE))));
		outdoorClimate.setSunBeamIntensity(
				lookupIntensity(api.getAsBigDecimal(diff.accessKeyXmlApi(Datapoint.TEMPERATURE))));
		outdoorClimate.setPlaceName(outside.getPlaceName());
		outdoorClimate.setDeviceThermometer(outside);
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
		roomClimate.setPlaceName(thermometer.getPlaceName());
		roomClimate.setDeviceThermometer(thermometer);
		return roomClimate;
	}

	private RoomClimate readRoomClimate(Device thermometer, Device heating) {
		RoomClimate roomClimate = readRoomClimate(thermometer);
		HeatingModel heatingModel = new HeatingModel();
		heatingModel.setBoostActive(api.getAsBigDecimal(heating.accessKeyXmlApi(Datapoint.CONTROL_MODE))
				.compareTo(HomematicConstants.HEATING_CONTROL_MODE_BOOST) == 0);
		heatingModel.setBoostMinutesLeft(
				api.getAsBigDecimal(heating.accessKeyXmlApi(Datapoint.BOOST_STATE)).intValue());
		heatingModel.setTargetTemperature(
				api.getAsBigDecimal(heating.accessKeyXmlApi(Datapoint.SET_TEMPERATURE)));
		heatingModel.setProgramNamePrefix(heating.programNamePrefix());
		roomClimate.setHeating(heatingModel);
		roomClimate.setPlaceName(thermometer.getPlaceName());
		roomClimate.setDeviceHeating(heating);

		return roomClimate;
	}

	private Window readWindow(Device shutter) { // TODO: D_U_M_M_Y
		Window window = new Window();
		window.setShutterDevice(shutter);
		window.setShutterPositionPercentage(30);
		window.setShutterPosition(ShutterPosition.fromPosition(window.getShutterPositionPercentage()));
		window.setShutterAutomation(true);
		window.setShutterAutomationInfoText("Dummy Text");
		return window;
	}

	private SwitchModel readSwitchState(Device device) {
		SwitchModel switchModel = new SwitchModel();
		switchModel.setState(api.getAsBoolean(device.accessKeyXmlApi(Datapoint.STATE)));
		switchModel.setDevice(device);
		switchModel.setAutomation(api.getAsBoolean(device.programNamePrefix() + "Automatic"));
		switchModel.setAutomationInfoText(api.getAsString(device.programNamePrefix() + "AutomaticInfoText"));
		return switchModel;
	}

	private PowerMeterModel readPowerConsumption(Device device) {

		PowerMeterModel model = new PowerMeterModel();
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
