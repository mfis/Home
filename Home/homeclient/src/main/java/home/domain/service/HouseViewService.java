package home.domain.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.ChartEntry;
import home.domain.model.ClimateView;
import home.domain.model.FrontDoorView;
import home.domain.model.PowerView;
import home.domain.model.ShutterView;
import home.domain.model.SwitchView;
import home.model.Message;
import home.model.MessageQueue;
import home.model.MessageType;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.CameraMode;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.FrontDoor;
import homecontroller.domain.model.Heating;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Intensity;
import homecontroller.domain.model.OutdoorClimate;
import homecontroller.domain.model.PowerMeter;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.ShutterPosition;
import homecontroller.domain.model.Switch;
import homecontroller.domain.model.Window;
import homelibrary.homematic.model.Device;

@Component
public class HouseViewService {

	private static final String AND_VALUE_IS = "&value=";

	private static final String AND_DEVICE_IS = "&deviceName=";

	private static final String TYPE_IS = "type=";

	private static final String COLOR_CLASS_RED = "danger";

	public static final String MESSAGEPATH = "/message?"; // NOSONAR

	private static final String TOGGLE_STATE = MESSAGEPATH + TYPE_IS + MessageType.TOGGLESTATE
			+ AND_DEVICE_IS;
	private static final String TOGGLE_AUTOMATION = MESSAGEPATH + TYPE_IS + MessageType.TOGGLEAUTOMATION
			+ AND_DEVICE_IS;

	private static final BigDecimal HIGH_TEMP = new BigDecimal("25");
	private static final BigDecimal LOW_TEMP = new BigDecimal("18");
	private static final BigDecimal FROST_TEMP = new BigDecimal("3");

	@Autowired
	private ViewFormatter viewFormatter;

	@PostConstruct
	public void init() {
		CompletableFuture.runAsync(() -> {
			try {
				Message message = new Message();
				message.setMessageType(MessageType.REFRESH_ALL_MODELS);
				MessageQueue.getInstance().request(message, false);
			} catch (Exception e) {
				LogFactory.getLog(HouseViewService.class)
						.error("Could not initialize HouseViewService completly.", e);
			}
		});
	}

	public void fillViewModel(Model model, HouseModel house, HistoryModel historyModel) {

		model.addAttribute("modelTimestamp", Long.toString(house.getDateTime()));

		formatClimate(model, "tempBathroom", house.getClimateBathRoom(), house.getHeatingBathRoom(), false);
		formatClimate(model, "tempKids", house.getClimateKidsRoom(), null, true);
		formatClimate(model, "tempLivingroom", house.getClimateLivingRoom(), null, false);
		formatClimate(model, "tempBedroom", house.getClimateBedRoom(), null, true);
		formatClimate(model, "tempLaundry", house.getClimateLaundry(), null, true);

		// formatWindow(model, "leftWindowBedroom",
		// house.getLeftWindowBedRoom()); // NOSONAR

		formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

		formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());

		formatFrontDoor(model, house.getFrontDoor(), Device.HAUSTUER_KAMERA);
		formatPower(model, house.getElectricalPowerConsumption(), historyModel);

		formatLowBattery(model, house.getLowBatteryDevices());

		formatWarnings(model, house);
	}

	public String lookupSunHeating(OutdoorClimate outdoorMaxClimate) {

		if (outdoorMaxClimate == null) {
			return StringUtils.EMPTY;
		}

		if (outdoorMaxClimate.getSunBeamIntensity().ordinal() >= outdoorMaxClimate
				.getSunHeatingInContrastToShadeIntensity().ordinal()) {
			return outdoorMaxClimate.getSunBeamIntensity().getSun();
		} else {
			return outdoorMaxClimate.getSunHeatingInContrastToShadeIntensity().getHeating();
		}
	}

	private void formatFrontDoor(Model model, FrontDoor frontDoor, Device device) {

		FrontDoorView frontDoorView = new FrontDoorView();

		if (frontDoor.getTimestampLastDoorbell() != null) {
			frontDoorView.setLastDoorbells(
					viewFormatter.formatPastTimestamp(frontDoor.getTimestampLastDoorbell(), true));
		} else {
			frontDoorView.setLastDoorbells("unbekannt");
		}
		frontDoorView.setIdLive("frontdoorcameralive");
		frontDoorView.setIdBell("frontdoorcamerabell");
		frontDoorView.setLinkLive(
				"/cameraPicture?deviceName=" + device.name() + "&cameraMode=" + CameraMode.LIVE + "&ts=");
		frontDoorView.setLinkLiveRequest("/cameraPictureRequest?type=" + MessageType.CAMERAPICTUREREQUEST
				+ AND_DEVICE_IS + device.name() + "&value=null");
		frontDoorView.setLinkBell("/cameraPicture?deviceName=" + device.name() + "&cameraMode="
				+ CameraMode.EVENT + "&ts=" + frontDoor.getTimestampLastDoorbell());

		model.addAttribute("frontDoor", frontDoorView);
	}

	private String format(BigDecimal val, boolean rounded) {
		if (val != null) {
			return new DecimalFormat("0." + (rounded ? "#" : "0")).format(val);
		} else {
			return null;
		}
	}

	private void formatClimate(Model model, String viewKey, Climate climate, Heating heating,
			boolean history) {
		ClimateView view = formatClimate(climate, heating, viewKey, history);
		model.addAttribute(viewKey, view);
	}

	private ClimateView formatClimate(Climate climate, Heating heating, String viewKey, boolean history) {

		ClimateView view = new ClimateView();

		if ((climate == null || climate.getTemperature() == null
				|| climate.getTemperature().getValue() == null)
				&& (climate == null || climate.getHumidity() == null
						|| climate.getHumidity().getValue() == null)) {
			view.setStateTemperature("unbekannt");
			return view;
		}

		view.setId(viewKey);
		view.setPlace(climate.getDevice().getPlace().getPlaceName());
		if (history) {
			view.setHistoryKey(climate.getDevice().programNamePrefix());
		}

		if (climate.getTemperature() != null) {
			// Temperature and humidity
			view.setStateTemperature(
					format(climate.getTemperature().getValue(), false) + ViewFormatter.DEGREE + "C");
			if (climate.getHumidity() != null) {
				view.setStateHumidity(format(climate.getHumidity().getValue(), true) + "%rH");
			}
			if (climate.getTemperature().getValue().compareTo(FROST_TEMP) < 0) {
				view.setStatePostfixIconTemperature("far fa-snowflake");
			}

			// Background color
			formatClimateBackground(climate, view);

			// Tendency icons
			formatClimateTendency(climate, view);

			// Heating
			formatClimateHeating(heating, view);

		} else {
			view.setStateTemperature("?");
		}

		if (climate instanceof RoomClimate) {
			for (String hintText : ((RoomClimate) climate).getHints().formatAsText(false, false, null)) {
				view.getHints().add(hintText);
			}
		}

		return view;
	}

	private void formatClimateTendency(Climate climate, ClimateView view) {

		if (climate.getTemperature().getTendency() != null) {
			view.setTendencyIconTemperature(climate.getTemperature().getTendency().getIconCssClass());
		}
		if (climate.getHumidity() != null && climate.getHumidity().getTendency() != null) {
			view.setTendencyIconHumidity(climate.getHumidity().getTendency().getIconCssClass());
		}
	}

	private void formatClimateHeating(Heating heating, ClimateView view) {

		if (heating != null) {
			if (heating.isBoostActive()) {
				view.setLinkBoost(String.valueOf(heating.getBoostMinutesLeft()));
				view.setColorClassHeating(COLOR_CLASS_RED);
			} else {
				view.setLinkBoost(MESSAGEPATH + TYPE_IS + MessageType.HEATINGBOOST + AND_DEVICE_IS
						+ heating.getDevice().name() + "&value=null");
			}
			view.setLinkManual(MESSAGEPATH + TYPE_IS + MessageType.HEATINGMANUAL + AND_DEVICE_IS
					+ heating.getDevice().name()); // value set in ui fragment
			view.setTargetTemp(format(heating.getTargetTemperature(), false));
			view.setHeatericon("fab fa-hotjar");
			view.setBusy(Boolean.toString(heating.isBusy()));
		}
	}

	private void formatClimateBackground(Climate climate, ClimateView view) {

		if (climate.getTemperature().getValue().compareTo(HIGH_TEMP) > 0) {
			view.setColorClass(COLOR_CLASS_RED);
			view.setIcon("fas fa-thermometer-full");
		} else if (climate.getTemperature().getValue().compareTo(LOW_TEMP) < 0) {
			view.setColorClass("info");
			view.setIcon("fas fa-thermometer-empty");
		} else {
			view.setColorClass("success");
			view.setIcon("fas fa-thermometer-half");
		}
	}

	private void formatFacadeTemperatures(Model model, String viewKeyMin, String viewKeyMax,
			HouseModel house) {

		ClimateView viewMin = formatClimate(house.getConclusionClimateFacadeMin(), null, viewKeyMin, false);
		ClimateView viewMax = new ClimateView();
		viewMax.setId(viewKeyMax);

		if (house.getConclusionClimateFacadeMin() != null) {
			viewMin.setPostfix(house.getConclusionClimateFacadeMin().getDevice().getPlace().getPlaceName());
			viewMin.setHistoryKey(house.getConclusionClimateFacadeMin().getDevice().programNamePrefix());
		}

		if (house.getConclusionClimateFacadeMax() != null) {
			viewMax.setStateTemperature(lookupSunHeating(house.getConclusionClimateFacadeMax()));
			viewMax.setName(
					"Fassade " + house.getConclusionClimateFacadeMax().getDevice().getPlace().getPlaceName());

			switch (Intensity.max(house.getConclusionClimateFacadeMax().getSunBeamIntensity(),
					house.getConclusionClimateFacadeMax().getSunHeatingInContrastToShadeIntensity())) {
			case NO:
				viewMin.setPostfix(""); // No sun, no heating -> no special
										// house
										// side name
				viewMax.setColorClass("secondary");
				break;
			case LOW:
				viewMax.setColorClass("success");
				viewMax.setIcon("far fa-sun");
				break;
			case MEDIUM:
				viewMax.setColorClass("warning");
				viewMax.setIcon("far fa-sun");
				break;
			case HIGH:
				viewMax.setColorClass(COLOR_CLASS_RED);
				viewMax.setIcon("fas fa-sun");
			}
		}

		model.addAttribute(viewKeyMin, viewMin);
		model.addAttribute(viewKeyMax, viewMax);
	}

	private void formatPower(Model model, PowerMeter powerMeter, HistoryModel historyModel) {

		PowerView power = new PowerView();
		power.setId(powerMeter.getDevice().programNamePrefix());
		power.setPlace(powerMeter.getDevice().getPlace().getPlaceName());
		power.setDescription(powerMeter.getDevice().getDescription());
		power.setHistoryKey(powerMeter.getDevice().programNamePrefix());
		power.setState(powerMeter.getActualConsumption().getValue().intValue() + " Watt");
		power.setName(powerMeter.getDevice().getType().getTypeName());
		power.setIcon("fas fa-bolt");
		if (powerMeter.getActualConsumption().getTendency() != null) {
			power.setTendencyIcon(powerMeter.getActualConsumption().getTendency().getIconCssClass());
		}

		if (!historyModel.getElectricPowerConsumptionDay().isEmpty()) {
			List<ChartEntry> dayViewModel = viewFormatter
					.fillPowerHistoryDayViewModel(historyModel.getElectricPowerConsumptionDay(), false);
			power.setTodayConsumption(dayViewModel.get(0));
		}

		model.addAttribute(powerMeter.getDevice().programNamePrefix(), power);
	}

	private void formatLowBattery(Model model, List<String> lowBatteryDevices) {
		model.addAttribute("lowBattery", lowBatteryDevices);
	}

	private void formatWarnings(Model model, HouseModel houseModel) {

		List<String> copy = new ArrayList<>(houseModel.getWarnings());
		long diff = new Date().getTime() - houseModel.getDateTime();
		if (diff > 1000 * 60 * 20) {
			copy.add("Letzte Aktualisierung vor " + (diff / 1000 / 60) + " Min.");
		}

		model.addAttribute("warnings", copy);
	}

	private void formatSwitch(Model model, String viewKey, Switch switchModel) {

		SwitchView view = new SwitchView();
		view.setId(viewKey);
		view.setName(switchModel.getDevice().getType().getTypeName());
		view.setState(switchModel.isState() ? "Eingeschaltet" : "Ausgeschaltet");
		if (switchModel.getAutomation() != null) {
			if (Boolean.TRUE.equals(switchModel.getAutomation())) {
				view.setState(view.getState() + ", automatisch");
				view.setLinkManual(TOGGLE_AUTOMATION + switchModel.getDevice().name() + AND_VALUE_IS
						+ AutomationState.MANUAL.name());
			} else {
				view.setState(view.getState() + ", manuell");
				view.setLinkAuto(TOGGLE_AUTOMATION + switchModel.getDevice().name() + AND_VALUE_IS
						+ AutomationState.AUTOMATIC.name());
			}
			view.setAutoInfoText(StringUtils.trimToEmpty(switchModel.getAutomationInfoText()));
		}
		view.setLabel(switchModel.isState() ? "ausschalten" : "einschalten");
		view.setIcon(switchModel.isState() ? "fas fa-toggle-on" : "fas fa-toggle-off");
		view.setLink(TOGGLE_STATE + switchModel.getDevice().name() + AND_VALUE_IS + !switchModel.isState());
		model.addAttribute(viewKey, view);
	}

	@SuppressWarnings("unused")
	private void formatWindow(Model model, String viewKey, Window windowModel) {

		ShutterView view = new ShutterView();
		view.setId(viewKey);
		view.setName(windowModel.getDevice().getType().getTypeName());
		view.setState(windowModel.getShutterPosition().getText(windowModel.getShutterPositionPercentage()));

		if (windowModel.getShutterAutomation() != null) {
			if (Boolean.TRUE.equals(windowModel.getShutterAutomation())) {
				view.setState(view.getState() + ", automatisch");
				view.setLinkManual(TOGGLE_AUTOMATION + windowModel.getDevice().name() + "&value=false");
			} else {
				view.setState(view.getState() + ", manuell");
				view.setLinkAuto(TOGGLE_AUTOMATION + windowModel.getDevice().name() + "&value=true");
			}
			view.setAutoInfoText(windowModel.getShutterAutomationInfoText());
		}

		view.setIcon(windowModel.getShutterPosition().getIcon());
		view.setIconOpen(ShutterPosition.OPEN.getIcon());
		view.setIconHalf(ShutterPosition.HALF.getIcon());
		view.setIconSunshade(ShutterPosition.SUNSHADE.getIcon());
		view.setIconClose(ShutterPosition.CLOSE.getIcon());

		view.setLinkClose(shutterLink(windowModel, ShutterPosition.CLOSE));
		view.setLinkOpen(shutterLink(windowModel, ShutterPosition.OPEN));
		view.setLinkHalf(shutterLink(windowModel, ShutterPosition.HALF));
		view.setLinkSunshade(shutterLink(windowModel, ShutterPosition.SUNSHADE));

		model.addAttribute(viewKey, view);
	}

	private String shutterLink(Window windowModel, ShutterPosition shutterPosition) {
		if (shutterPosition == windowModel.getShutterPosition()) {
			return "#";
		} else {
			return MESSAGEPATH + TYPE_IS + MessageType.SHUTTERPOSITION + AND_DEVICE_IS
					+ windowModel.getDevice().name() + AND_VALUE_IS + shutterPosition.getControlPosition();
		}
	}

}
