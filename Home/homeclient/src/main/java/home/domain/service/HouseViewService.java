package home.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.ClimateView;
import home.domain.model.FrontDoorView;
import home.domain.model.HistoryEntry;
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
import homecontroller.domain.model.PowerConsumptionMonth;
import homecontroller.domain.model.PowerMeter;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.ShutterPosition;
import homecontroller.domain.model.Switch;
import homecontroller.domain.model.TemperatureHistory;
import homecontroller.domain.model.Window;
import homelibrary.homematic.model.Device;

@Component
public class HouseViewService {

	private static final String COLOR_CLASS_RED = "danger";

	public static final String MESSAGEPATH = "/message?"; // NOSONAR

	private static final String DEGREE = "\u00b0";
	private static final String TOGGLE_STATE = MESSAGEPATH + "type=" + MessageType.TOGGLESTATE
			+ "&deviceName=";
	private static final String TOGGLE_AUTOMATION = MESSAGEPATH + "type=" + MessageType.TOGGLEAUTOMATION
			+ "&deviceName=";

	private static final BigDecimal HIGH_TEMP = new BigDecimal("25");
	private static final BigDecimal LOW_TEMP = new BigDecimal("18");
	private static final BigDecimal FROST_TEMP = new BigDecimal("3");

	private static final int COMPARE_PERCENTAGE_GREEN_UNTIL = -1;
	private static final int COMPARE_PERCENTAGE_GRAY_UNTIL = +2;
	private static final int COMPARE_PERCENTAGE_ORANGE_UNTIL = +15;

	private static final BigDecimal BD100 = new BigDecimal(100);
	private static final long KWH_FACTOR = 1000L;

	private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");
	private static final DateTimeFormatter DAY_MONTH_YEAR_FORMATTER = DateTimeFormatter
			.ofPattern("dd.MM.yyyy");
	private static final DateTimeFormatter WEEKDAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE",
			Locale.GERMAN);
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	@Autowired
	private Environment env;

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

	public void fillViewModel(Model model, HouseModel house) {

		model.addAttribute("modelTimestamp", Long.toString(house.getDateTime()));

		formatClimate(model, "tempBathroom", house.getClimateBathRoom(), house.getHeatingBathRoom(), false);
		formatClimate(model, "tempKids", house.getClimateKidsRoom(), null, true);
		formatClimate(model, "tempLivingroom", house.getClimateLivingRoom(), null, false);
		formatClimate(model, "tempBedroom", house.getClimateBedRoom(), null, true);
		formatClimate(model, "tempLaundry", house.getClimateLaundry(), null, true);

		// formatWindow(model, "leftWindowBedroom",
		// house.getLeftWindowBedRoom());

		formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

		formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());

		formatFrontDoor(model, house.getFrontDoor(), Device.HAUSTUER_KAMERA);
		formatPower(model, house.getElectricalPowerConsumption());

		formatLowBattery(model, house.getLowBatteryDevices());
	}

	public void fillHistoryViewModel(Model model, HistoryModel history, HouseModel house, String key) {
		if (key.equals(house.getElectricalPowerConsumption().getDevice().programNamePrefix())) {
			fillPowerHistoryViewModel(model, history);
		} else if (key.equals(house.getConclusionClimateFacadeMin().getDevice().programNamePrefix())) {
			fillTemperatureHistoryViewModel(model, history.getOutsideTemperature());
		} else if (key.equals(house.getClimateBedRoom().getDevice().programNamePrefix())) {
			fillTemperatureHistoryViewModel(model, history.getBedRoomTemperature());
		} else if (key.equals(house.getClimateKidsRoom().getDevice().programNamePrefix())) {
			fillTemperatureHistoryViewModel(model, history.getKidsRoomTemperature());
		} else if (key.equals(house.getClimateLaundry().getDevice().programNamePrefix())) {
			fillTemperatureHistoryViewModel(model, history.getLaundryTemperature());
		}
	}

	private void fillPowerHistoryViewModel(Model model, HistoryModel history) {

		List<HistoryEntry> list = new LinkedList<>();
		DecimalFormat decimalFormat = new DecimalFormat("0");
		int index = 0;
		for (PowerConsumptionMonth pcm : history.getElectricPowerConsumption()) {
			if (pcm.getPowerConsumption() != null) {
				HistoryEntry entry = new HistoryEntry();
				Long calculated = null;
				entry.setLineOneLabel(MONTH_YEAR_FORMATTER.format(pcm.measurePointMaxDateTime()));
				entry.setLineOneValue(decimalFormat.format(pcm.getPowerConsumption() / KWH_FACTOR) + " kW/h");
				if (index < history.getElectricPowerConsumption().size() - 3) {
					entry.setCollapse(" collapse multi-collapse historyTarget");
				}
				boolean calculateDifference = true;
				if (index == history.getElectricPowerConsumption().size() - 1) {
					if (pcm.measurePointMaxDateTime().getDayOfMonth() > 1) {
						entry.setLineTwoLabel("Hochgerechnet");
						entry.setBadgeLabel("Vergleich Vorjahr");
						calculated = calculateProjectedConsumption(entry, pcm.measurePointMaxDateTime(), pcm);
					} else {
						calculateDifference = false;
					}
					entry.setColorClass(" list-group-item-secondary");
					entry.setLineOneLabel(entry.getLineOneLabel() + " bisher");
				}
				if (calculateDifference) {
					calculatePreviousYearDifference(entry, pcm, history.getElectricPowerConsumption(),
							pcm.getPowerConsumption(), calculated);
				}
				list.add(entry);
			}
			index++;
		}

		Collections.reverse(list);
		model.addAttribute("historyEntries", list);
	}

	private void formatFrontDoor(Model model, FrontDoor frontDoor, Device device) {

		FrontDoorView frontDoorView = new FrontDoorView();

		if (frontDoor.getTimestampLastDoorbell() != null) {
			frontDoorView.setLastDoorbells(formatPastTimestamp(frontDoor.getTimestampLastDoorbell()));
		} else {
			frontDoorView.setLastDoorbells("unbekannt");
		}
		frontDoorView.setIdLive("frontdoorcameralive");
		frontDoorView.setIdBell("frontdoorcamerabell");
		frontDoorView.setLinkLive(
				"/cameraPicture?deviceName=" + device.name() + "&cameraMode=" + CameraMode.LIVE + "&ts=");
		frontDoorView.setLinkLiveRequest("/cameraPictureRequest?type=" + MessageType.CAMERAPICTUREREQUEST
				+ "&deviceName=" + device.name() + "&value=null");
		frontDoorView.setLinkBell("/cameraPicture?deviceName=" + device.name() + "&cameraMode="
				+ CameraMode.EVENT + "&ts=" + frontDoor.getTimestampLastDoorbell());

		model.addAttribute("frontDoor", frontDoorView);
	}

	private String formatPastTimestamp(long date) {

		LocalDateTime localDate1 = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
				.toLocalDateTime();
		LocalDateTime localDate2 = LocalDateTime.now();

		long between = ChronoUnit.DAYS.between(localDate1.truncatedTo(ChronoUnit.DAYS),
				localDate2.truncatedTo(ChronoUnit.DAYS));

		String dayString;
		if (between == 0) {
			dayString = "Heute";
		} else if (between == 1) {
			dayString = "Gestern";
		} else if (between == 2) {
			dayString = "Vorgestern";
		} else if (between > -1 && between < 7) {
			dayString = WEEKDAY_FORMATTER.format(localDate1);
		} else {
			dayString = DAY_MONTH_YEAR_FORMATTER.format(localDate1);
		}
		dayString += ", " + TIME_FORMATTER.format(localDate1) + " Uhr";
		return dayString;
	}

	private void fillTemperatureHistoryViewModel(Model model, List<TemperatureHistory> historyList) {

		List<HistoryEntry> list = new LinkedList<>();
		int index = 0;
		for (TemperatureHistory th : historyList) {
			HistoryEntry entry = new HistoryEntry();
			entry.setLineOneValueIcon("fas fa-moon");
			entry.setLineTwoValueIcon("fas fa-sun");
			LocalDate date = Instant.ofEpochMilli(th.getDate()).atZone(ZoneId.systemDefault()).toLocalDate();
			if (th.isSingleDay()) {
				if (date.compareTo(LocalDate.now()) == 0) {
					entry.setLineOneLabel("Heute");
				} else if (date.compareTo(LocalDate.now().minusDays(1)) == 0) {
					entry.setLineOneLabel("Gestern");
				} else {
					entry.setLineOneLabel(DAY_MONTH_YEAR_FORMATTER.format(date));
				}
				entry.setColorClass(" list-group-item-secondary");
			} else {
				entry.setLineOneLabel(MONTH_YEAR_FORMATTER.format(date));
			}
			entry.setLineOneValue(formatTemperatures(th.getNightMin(), th.getNightMax()));
			entry.setLineTwoValue(formatTemperatures(th.getDayMin(), th.getDayMax()));
			if (index > 2) {
				entry.setCollapse(" collapse multi-collapse historyTarget");
			}
			list.add(entry);
			index++;
		}
		model.addAttribute("historyEntries", list);
	}

	private String formatTemperatures(BigDecimal min, BigDecimal max) {

		if (min == null || max == null) {
			return "n/a";
		}

		String minFrmt = formatTemperature(min);
		String maxFrmt = formatTemperature(max);

		if (StringUtils.equals(minFrmt, maxFrmt)) {
			return minFrmt + DEGREE + "C";
		}

		return minFrmt + DEGREE + "C bis " + maxFrmt + DEGREE + "C";
	}

	private String formatTemperature(BigDecimal value) {

		DecimalFormat decimalFormat = new DecimalFormat("0");
		String frmt = decimalFormat.format(value);
		if ("-0".equals(frmt)) { // special case: some negative value roundet to
									// zero has a leading '-'
			frmt = "0";
		}
		return frmt;
	}

	private void calculatePreviousYearDifference(HistoryEntry entry, PowerConsumptionMonth pcm,
			List<PowerConsumptionMonth> history, Long actual, Long calculated) {

		DecimalFormat decimalFormat = new DecimalFormat("+0;-0");
		LocalDateTime baseDateTime = pcm.measurePointMaxDateTime();
		Long baseValue = calculated != null ? calculated : actual;
		Long compareValue = null;

		for (PowerConsumptionMonth historyEntry : history) {
			LocalDateTime otherDateTime = historyEntry.measurePointMaxDateTime();
			if (otherDateTime.getYear() + 1 == baseDateTime.getYear()
					&& otherDateTime.getMonthValue() == baseDateTime.getMonthValue()) {
				compareValue = historyEntry.getPowerConsumption();
				break;
			}
		}

		if (baseValue != null && compareValue != null) {
			long diff = baseValue - compareValue;
			BigDecimal percentage = new BigDecimal(diff)
					.divide(new BigDecimal(baseValue), 4, RoundingMode.HALF_UP).multiply(BD100);
			entry.setBadgeValue(decimalFormat.format(percentage) + "%");
			if (percentage.intValue() <= COMPARE_PERCENTAGE_GREEN_UNTIL) {
				entry.setBadgeClass("badge-success");
			} else if (percentage.intValue() <= COMPARE_PERCENTAGE_GRAY_UNTIL) {
				entry.setBadgeClass("badge-secondary");
			} else if (percentage.intValue() <= COMPARE_PERCENTAGE_ORANGE_UNTIL) {
				entry.setBadgeClass("badge-warning");
			} else {
				entry.setBadgeClass("badge-danger");
			}
		}
	}

	private Long calculateProjectedConsumption(HistoryEntry entry, LocalDateTime dateTime,
			PowerConsumptionMonth pcm) {

		YearMonth yearMonthObject = YearMonth.of(dateTime.getYear(), dateTime.getMonthValue());
		int daysInMonth = yearMonthObject.lengthOfMonth();
		int hoursAgo = ((dateTime.getDayOfMonth() - 1) * 24) + dateTime.getHour();
		int hoursToGo = (daysInMonth * 24) - hoursAgo;
		if (hoursAgo > 0) {
			BigDecimal actualValue = new BigDecimal(pcm.getPowerConsumption());
			BigDecimal calculated = actualValue
					.add(actualValue.divide(new BigDecimal(hoursAgo), 2, RoundingMode.HALF_UP)
							.multiply(new BigDecimal(hoursToGo)));
			entry.setLineTwoValue(
					"≈" + new DecimalFormat("0").format(calculated.longValue() / KWH_FACTOR) + " kW/h");
			return calculated.longValue();
		}
		return null;
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
			view.setStateTemperature(format(climate.getTemperature().getValue(), false) + DEGREE + "C");
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
				view.setLinkBoost(MESSAGEPATH + "type=" + MessageType.HEATINGBOOST + "&deviceName="
						+ heating.getDevice().name() + "&value=null");
			}
			view.setLinkManual(MESSAGEPATH + "type=" + MessageType.HEATINGMANUAL + "&deviceName="
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

	private void formatPower(Model model, PowerMeter powerMeter) {

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
		model.addAttribute(powerMeter.getDevice().programNamePrefix(), power);
	}

	private void formatLowBattery(Model model, List<String> lowBatteryDevices) {
		model.addAttribute("lowBattery", lowBatteryDevices);
	}

	private void formatSwitch(Model model, String viewKey, Switch switchModel) {

		SwitchView view = new SwitchView();
		view.setId(viewKey);
		view.setName(switchModel.getDevice().getType().getTypeName());
		view.setState(switchModel.isState() ? "Eingeschaltet" : "Ausgeschaltet");
		if (switchModel.getAutomation() != null) {
			if (switchModel.getAutomation()) {
				view.setState(view.getState() + ", automatisch");
				view.setLinkManual(TOGGLE_AUTOMATION + switchModel.getDevice().name() + "&value="
						+ AutomationState.MANUAL.name());
			} else {
				view.setState(view.getState() + ", manuell");
				view.setLinkAuto(TOGGLE_AUTOMATION + switchModel.getDevice().name() + "&value="
						+ AutomationState.AUTOMATIC.name());
			}
			view.setAutoInfoText(StringUtils.trimToEmpty(switchModel.getAutomationInfoText()));
		}
		view.setLabel(switchModel.isState() ? "ausschalten" : "einschalten");
		view.setIcon(switchModel.isState() ? "fas fa-toggle-on" : "fas fa-toggle-off");
		view.setLink(TOGGLE_STATE + switchModel.getDevice().name() + "&value=" + !switchModel.isState());
		model.addAttribute(viewKey, view);
	}

	private void formatWindow(Model model, String viewKey, Window windowModel) {

		ShutterView view = new ShutterView();
		view.setId(viewKey);
		view.setName(windowModel.getDevice().getType().getTypeName());
		view.setState(windowModel.getShutterPosition().getText(windowModel.getShutterPositionPercentage()));

		if (windowModel.getShutterAutomation() != null) {
			if (windowModel.getShutterAutomation()) {
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
			return MESSAGEPATH + "type=" + MessageType.SHUTTERPOSITION + "&deviceName="
					+ windowModel.getDevice().name() + "&value=" + shutterPosition.getControlPosition();
		}
	}

	public void fillLinks(Model model) {
		model.addAttribute("link_internet_ccu2", env.getProperty("link.internet.ccu2"));
		model.addAttribute("link_local_ccu2", env.getProperty("link.local.ccu2"));
		model.addAttribute("link_internet_historian", env.getProperty("link.internet.historian"));
		model.addAttribute("link_local_historian", env.getProperty("link.local.historian"));

	}

}
