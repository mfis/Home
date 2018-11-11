package homecontroller.domain.service;

import java.math.BigDecimal;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import homecontroller.dao.ModelDAO;
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
import homecontroller.domain.model.SwitchModel;
import homecontroller.service.HomematicAPI;
import homecontroller.service.PushService;

@Component
public class HouseService {

	private final static BigDecimal TARGET_TEMPERATURE_INSIDE = new BigDecimal("22");

	private final static BigDecimal SUN_INTENSITY_NO = new BigDecimal("3");
	private final static BigDecimal SUN_INTENSITY_LOW = new BigDecimal("8");
	private final static BigDecimal SUN_INTENSITY_MEDIUM = new BigDecimal("15");

	private final static long HINT_TIMEOUT_MINUTES_AFTER_BOOST = 90L;

	private final static Object REFRESH_MONITOR = new Object();
	private final static long REFRESH_TIMEOUT = 5 * 1000; // 5 sec

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
		calculateConclusion(newModel);
		ModelDAO.getInstance().write(newModel);

		if (notify) {
			synchronized (REFRESH_MONITOR) {
				REFRESH_MONITOR.notify();
			}
		}

		pushService.send(oldModel, newModel);
	}

	private HouseModel refreshModel() {

		api.refresh();

		HouseModel newModel = new HouseModel();

		newModel.setClimateBathRoom(readRoomClimate(Device.THERMOSTAT_BAD, Device.THERMOSTAT_BAD));
		newModel.setClimateKidsRoom(readRoomClimate(Device.THERMOMETER_KINDERZIMMER));
		newModel.setClimateLivingRoom(readRoomClimate(Device.THERMOMETER_WOHNZIMMER));
		newModel.setClimateBedRoom(readRoomClimate(Device.THERMOMETER_SCHLAFZIMMER));

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

	public void calculateConclusion(HouseModel newModel) {

		if (newModel.getClimateTerrace().getTemperature()
				.compareTo(newModel.getClimateEntrance().getTemperature()) < 0) {
			newModel.setConclusionClimateFacadeMin(newModel.getClimateTerrace());
			newModel.setConclusionClimateFacadeMax(newModel.getClimateEntrance());
		} else {
			newModel.setConclusionClimateFacadeMin(newModel.getClimateEntrance());
			newModel.setConclusionClimateFacadeMax(newModel.getClimateTerrace());
		}

		BigDecimal sunShadeDiff = newModel.getConclusionClimateFacadeMax().getTemperature()
				.subtract(newModel.getConclusionClimateFacadeMin().getTemperature()).abs();
		newModel.getConclusionClimateFacadeMax()
				.setSunHeatingInContrastToShadeIntensity(lookupIntensity(sunShadeDiff));

		lookupHint(newModel.getClimateKidsRoom(), newModel.getClimateEntrance());
		lookupHint(newModel.getClimateBathRoom(), newModel.getClimateEntrance());
		lookupHint(newModel.getClimateBedRoom(), newModel.getClimateTerrace());
		lookupHint(newModel.getClimateLivingRoom(), newModel.getClimateTerrace());

	}

	private void lookupHint(RoomClimate room, OutdoorClimate outdoor) {

		if (room.getTemperature() == null) {
			return;
		} else if (room.getTemperature().compareTo(TARGET_TEMPERATURE_INSIDE) < 0) {
			// TODO: using sun heating in the winter for warming up rooms
			return;
		} else if (room.getTemperature().compareTo(TARGET_TEMPERATURE_INSIDE) > 0
				&& outdoor.getTemperature().compareTo(room.getTemperature()) < 0
				&& outdoor.getSunBeamIntensity().ordinal() <= Intensity.LOW.ordinal()) {
			if (room.getHeating() != null && (room.getHeating().isBoostActive()
					|| room.getHeating().getTargetTemperature().compareTo(TARGET_TEMPERATURE_INSIDE) > 0
					|| historyDAO.minutesSinceLastHeatingBoost(room) < HINT_TIMEOUT_MINUTES_AFTER_BOOST)) {
				return;
			} else {
				room.setHint(Hint.OPEN_WINDOW);
			}
		} else if (room.getTemperature().compareTo(TARGET_TEMPERATURE_INSIDE) > 0
				&& outdoor.getSunBeamIntensity().ordinal() > Intensity.LOW.ordinal()) {
			room.setHint(Hint.CLOSE_ROLLER_SHUTTER);
		}

		return;
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

	public void toggle(String devIdVar) throws Exception {
		api.toggleBooleanState(devIdVar);
		refreshHouseModel(false);
	}

	public synchronized void heatingBoost(String prefix) throws Exception {
		api.runProgram(prefix + "Boost");
		synchronized (REFRESH_MONITOR) {
			REFRESH_MONITOR.wait(REFRESH_TIMEOUT);
		}
	}

	// needs to be synchronized because of using ccu-systemwide temperature
	// variable
	public synchronized void heatingManual(String prefix, String temperature) throws Exception {
		temperature = StringUtils.replace(temperature, ",", "."); // decimalpoint
		api.changeValue(prefix + "Temperature", temperature);
		api.runProgram(prefix + "Manual");
		synchronized (REFRESH_MONITOR) {
			REFRESH_MONITOR.wait(REFRESH_TIMEOUT);
		}
	}

	private OutdoorClimate readOutdoorClimate(Device outside, Device diff) {
		OutdoorClimate outdoorClimate = new OutdoorClimate();
		outdoorClimate.setTemperature(api.getAsBigDecimal(outside.accessKeyXmlApi(Datapoint.TEMPERATURE)));
		outdoorClimate.setSunBeamIntensity(
				lookupIntensity(api.getAsBigDecimal(diff.accessKeyXmlApi(Datapoint.TEMPERATURE))));
		outdoorClimate.setPlaceName(outside.getPlaceName());
		outdoorClimate.setDeviceThermometer(outside);
		return outdoorClimate;
	}

	private RoomClimate readRoomClimate(Device thermometer) {
		RoomClimate roomClimate = new RoomClimate();
		roomClimate.setTemperature(
				api.getAsBigDecimal(thermometer.accessKeyXmlApi(Datapoint.ACTUAL_TEMPERATURE)));
		roomClimate.setHumidity(api.getAsBigDecimal(thermometer.accessKeyXmlApi(Datapoint.HUMIDITY)));
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
		model.setActualConsumption(api.getAsBigDecimal(device.accessKeyXmlApi(Datapoint.POWER)).intValue());
		return model;
	}

	private void checkLowBattery(HouseModel model, Device device) {

		Boolean state = null;
		if (device.isHomematic()) {
			state = api.getAsBoolean(device.accessMainDeviceKeyXmlApi(Datapoint.LOWBAT));
		} else if (device.isHomematicIP()) {
			state = api.getAsBoolean(device.accessMainDeviceKeyXmlApi(Datapoint.LOW_BAT));
		}

		if (state != null && state == true) {
			model.getLowBatteryDevices().add(device.getDescription());
		}
	}
}
