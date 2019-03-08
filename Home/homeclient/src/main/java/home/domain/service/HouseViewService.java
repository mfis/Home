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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.ClimateView;
import home.domain.model.HistoryEntry;
import home.domain.model.PowerView;
import home.domain.model.ShutterView;
import home.domain.model.SwitchView;
import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.Heating;
import homecontroller.domain.model.Hint;
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

@Component
public class HouseViewService {

	private static final String TOGGLE_STATE = "/togglestate?deviceName=";
	private static final String TOGGLE_AUTOMATION = "/toggleautomation?deviceName=";
	
	private static final BigDecimal HIGH_TEMP = new BigDecimal("25");
	private static final BigDecimal LOW_TEMP = new BigDecimal("19");
	private static final BigDecimal FROST_TEMP = new BigDecimal("3");

	private static final int COMPARE_PERCENTAGE_GREEN_UNTIL = -1;
	private static final int COMPARE_PERCENTAGE_GRAY_UNTIL = +2;
	private static final int COMPARE_PERCENTAGE_ORANGE_UNTIL = +15;

	private static final BigDecimal BD100 = new BigDecimal(100);
	private static final long KWH_FACTOR = 1000L;

	private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");
	
	private static final DateTimeFormatter DAY_MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	@Autowired
	private Environment env;

	public void fillViewModel(Model model, HouseModel house) {

		formatClimate(model, "tempBathroom", house.getClimateBathRoom(), house.getHeatingBathRoom());
		formatClimate(model, "tempKids", house.getClimateKidsRoom(), null);
		formatClimate(model, "tempLivingroom", house.getClimateLivingRoom(), null);
		formatClimate(model, "tempBedroom", house.getClimateBedRoom(), null);

		formatWindow(model, "leftWindowBedroom", house.getLeftWindowBedRoom());

		formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

		formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());
		formatPower(model, house.getElectricalPowerConsumption());

		formatLowBattery(model, house.getLowBatteryDevices());
	}

	public void fillHistoryViewModel(Model model, HistoryModel history) {
		fillPowerHistoryViewModel(model, history);
		fillOutsideTemperatureHistoryViewModel(model, history);
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
					entry.setCollapse(" collapse multi-collapse electricity");
				}
				boolean calculateDifference = true;
				if (index == history.getElectricPowerConsumption().size() - 1) {
					if (pcm.measurePointMaxDateTime().getDayOfMonth() > 1) {
						entry.setLineTwoLabel("Hochgerechnet");
						entry.setBadgeLabel("Vergleich Vorjahr");
						calculated = calculateProjectedConsumption(entry, pcm.measurePointMaxDateTime(), pcm);
					}else {
						calculateDifference = false;
					}
					entry.setColorClass(" list-group-item-secondary");
					entry.setLineOneLabel(entry.getLineOneLabel() + " bisher");
				}
				if(calculateDifference) {
					calculatePreviousYearDifference(entry, pcm, history.getElectricPowerConsumption(),
							pcm.getPowerConsumption(), calculated);
				}
				list.add(entry);
			}
			index++;
		}

		Collections.reverse(list);
		model.addAttribute("power", list);
	}
	
	private void fillOutsideTemperatureHistoryViewModel(Model model, HistoryModel history) {
		
		List<HistoryEntry> list = new LinkedList<>();
		int index = 0;
		for (TemperatureHistory th : history.getOutsideTemperature()) {
				HistoryEntry entry = new HistoryEntry();
				entry.setLineOneValueIcon("fas fa-moon");
				entry.setLineTwoValueIcon("fas fa-sun");
				LocalDate date =
					    Instant.ofEpochMilli(th.getDate()).atZone(ZoneId.systemDefault()).toLocalDate();
				if(th.isSingleDay()) {
					if(date.compareTo(LocalDate.now())==0) {
						entry.setLineOneLabel("Heute");
					}else if(date.compareTo(LocalDate.now().minusDays(1))==0) {
						entry.setLineOneLabel("Gestern");
					}else {
						entry.setLineOneLabel(DAY_MONTH_YEAR_FORMATTER.format(date));
					}
					entry.setColorClass(" list-group-item-secondary");
				}else {
					entry.setLineOneLabel(MONTH_YEAR_FORMATTER.format(date));
				}
				entry.setLineOneValue(formatTemperatures(th.getNightMin(), th.getNightMax()));
				entry.setLineTwoValue(formatTemperatures(th.getDayMin(), th.getDayMax()));
				if (index >2) {
					entry.setCollapse(" collapse multi-collapse temperatureOutside");
				}
				list.add(entry);
			index++;
		}

		model.addAttribute("temperatureOutside", list);
	}
	
	private String formatTemperatures(BigDecimal min, BigDecimal max) {
		
		if(min==null && max==null) {
			return "n/a";
		}
		
		DecimalFormat decimalFormat = new DecimalFormat("0");
		if(min.compareTo(max)==0) {
			return decimalFormat.format(min) + "\u00b0" + "C";
		}
		
		return decimalFormat.format(min)+ "\u00b0" + "C bis " + decimalFormat.format(max) + "\u00b0" + "C";
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

	private void formatClimate(Model model, String viewKey, Climate climate, Heating heating) {
		ClimateView view = formatClimate(climate, heating, viewKey);
		model.addAttribute(viewKey, view);
	}

	private ClimateView formatClimate(Climate climate, Heating heating, String viewKey) {

		ClimateView view = new ClimateView();
		view.setId(viewKey);

		if (climate.getTemperature() != null
				&& climate.getTemperature().getValue().compareTo(BigDecimal.ZERO) == 0
				&& climate.getHumidity() != null
				&& climate.getHumidity().getValue().compareTo(BigDecimal.ZERO) == 0) {
			view.setStateTemperature("unbekannt");
			return view;
		}

		if (climate.getTemperature() != null) {
			// Temperature and humidity
			view.setStateTemperature(format(climate.getTemperature().getValue(), false) + "\u00b0" + "C");
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
			for (Hint hint : ((RoomClimate) climate).getHints()) {
				view.getHints().add(hint.getText());
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
			} else {
				view.setLinkBoost("/heatingboost?deviceName=" + heating.getDevice().name());
			}
			view.setLinkManual("/heatingmanual?deviceName=" + heating.getDevice().name());
			view.setTargetTemp(format(heating.getTargetTemperature(), false));
			view.setHeatericon("fab fa-hotjar");
		}
	}

	private void formatClimateBackground(Climate climate, ClimateView view) {

		if (climate.getTemperature().getValue().compareTo(HIGH_TEMP) > 0) {
			view.setColorClass("danger");
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

		ClimateView viewMin = formatClimate(house.getConclusionClimateFacadeMin(), null, viewKeyMin);
		ClimateView viewMax = new ClimateView();
		viewMax.setId(viewKeyMax);

		viewMin.setPostfix(house.getConclusionClimateFacadeMin().getDevice().getPlace().getPlaceName());

		viewMax.setStateTemperature(lookupSunHeating(house.getConclusionClimateFacadeMax()));
		viewMax.setName(
				"Fassade " + house.getConclusionClimateFacadeMax().getDevice().getPlace().getPlaceName());

		switch (Intensity.max(house.getConclusionClimateFacadeMax().getSunBeamIntensity(),
				house.getConclusionClimateFacadeMax().getSunHeatingInContrastToShadeIntensity())) {
		case NO:
			viewMin.setPostfix(""); // No sun, no heating -> no special house
									// side name
			viewMax.setColorClass("secondary");
			break;
		case LOW:
			viewMax.setColorClass("dark");
			viewMax.setIcon("far fa-sun");
			break;
		case MEDIUM:
			viewMax.setColorClass("warning");
			viewMax.setIcon("far fa-sun");
			break;
		case HIGH:
			viewMax.setColorClass("danger");
			viewMax.setIcon("fas fa-sun");
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
				view.setLinkManual(
						TOGGLE_AUTOMATION + switchModel.getDevice().name() + "&automationStateValue=" + AutomationState.MANUAL.name());
			} else {
				view.setState(view.getState() + ", manuell");
				view.setLinkAuto(TOGGLE_AUTOMATION + switchModel.getDevice().name() + "&automationStateValue=" + AutomationState.AUTOMATIC.name());
			}
			view.setAutoInfoText(StringUtils.trimToEmpty(switchModel.getAutomationInfoText()));
		}
		view.setLabel(switchModel.isState() ? "ausschalten" : "einschalten");
		view.setIcon(switchModel.isState() ? "fas fa-toggle-on" : "fas fa-toggle-off");
		view.setLink(TOGGLE_STATE + switchModel.getDevice().name() + "&booleanValue=" + !switchModel.isState());
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
				view.setLinkManual(
						TOGGLE_AUTOMATION + windowModel.getDevice().name() + "&booleanValue=false");
			} else {
				view.setState(view.getState() + ", manuell");
				view.setLinkAuto(TOGGLE_AUTOMATION + windowModel.getDevice().name() + "&booleanValue=true");
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
			return "/shutterSetPosition?deviceName=" + windowModel.getDevice().name()
					+ "&shutterSetPosition=" + shutterPosition.getControlPosition();
		}
	}

	public void fillLinks(Model model) {
		model.addAttribute("link_internet_ccu2", env.getProperty("link.internet.ccu2"));
		model.addAttribute("link_local_ccu2", env.getProperty("link.local.ccu2"));
		model.addAttribute("link_internet_historian", env.getProperty("link.internet.historian"));
		model.addAttribute("link_local_historian", env.getProperty("link.local.historian"));

	}

}
