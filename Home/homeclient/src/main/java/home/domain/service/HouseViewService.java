package home.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
import home.domain.model.PowerHistoryEntry;
import home.domain.model.PowerView;
import home.domain.model.ShutterView;
import home.domain.model.SwitchView;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.Datapoint;
import homecontroller.domain.model.Hint;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Intensity;
import homecontroller.domain.model.PowerConsumptionMonth;
import homecontroller.domain.model.PowerMeterModel;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.ShutterPosition;
import homecontroller.domain.model.SwitchModel;
import homecontroller.domain.model.Window;

@Component
public class HouseViewService {

	private static final String TOGGLE_DEV_ID_VAR = "/toggle?devIdVar=";
	private static final String AUTOMATIC = "Automatic";
	private static final BigDecimal HIGH_TEMP = new BigDecimal("25");
	private static final BigDecimal LOW_TEMP = new BigDecimal("19");
	private static final BigDecimal FROST_TEMP = new BigDecimal("3");

	private static final long KWH_FACTOR = 1000L;
	private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

	@Autowired
	private Environment env;

	public void fillViewModel(Model model, HouseModel house) {

		formatClimate(model, "tempBathroom", house.getClimateBathRoom());
		formatClimate(model, "tempKids", house.getClimateKidsRoom());
		formatClimate(model, "tempLivingroom", house.getClimateLivingRoom());
		formatClimate(model, "tempBedroom", house.getClimateBedRoom());

		formatWindow(model, "leftWindowBedroom", house.getLeftWindowBedRoom());

		formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

		formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());
		formatPower(model, house.getElectricalPowerConsumption());

		formatLowBattery(model, house.getLowBatteryDevices());
	}

	public void fillHistoryViewModel(Model model, HistoryModel history) {

		List<PowerHistoryEntry> list = new LinkedList<>();
		DecimalFormat decimalFormat = new DecimalFormat("0");
		int index = 0;
		for (PowerConsumptionMonth pcm : history.getElectricPowerConsumption()) {
			if (pcm.getPowerConsumption() != null) {
				PowerHistoryEntry entry = new PowerHistoryEntry();
				entry.setKey(MONTH_YEAR_FORMATTER.format(pcm.measurePointMaxDateTime()));
				entry.setValue(decimalFormat.format(pcm.getPowerConsumption() / KWH_FACTOR) + " kW/h");
				if (index < history.getElectricPowerConsumption().size() - 3) {
					entry.setCollapse(" collapse multi-collapse electricity");
				}
				if (index == history.getElectricPowerConsumption().size() - 1) {
					if (pcm.measurePointMaxDateTime().getDayOfMonth() > 1) {
						calculateProjectedConsumption(entry, pcm.measurePointMaxDateTime(), pcm);
					}
					entry.setColorClass(" list-group-item-secondary");
					entry.setKey(entry.getKey() + " bisher");
				}
				list.add(entry);
			}
			index++;
		}

		Collections.reverse(list);
		model.addAttribute("power", list);
	}

	private void calculateProjectedConsumption(PowerHistoryEntry entry, LocalDateTime dateTime,
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
			entry.setCalculated(new DecimalFormat("0").format(calculated.longValue() / KWH_FACTOR) + " kW/h");
		}
	}

	private String format(BigDecimal val, boolean rounded) {
		if (val != null) {
			return new DecimalFormat("0." + (rounded ? "#" : "0")).format(val);
		} else {
			return null;
		}
	}

	private void formatClimate(Model model, String viewKey, Climate climate) {
		ClimateView view = formatClimate(climate, viewKey);
		model.addAttribute(viewKey, view);
	}

	private ClimateView formatClimate(Climate climate, String viewKey) {

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
			formatClimateHeating(climate, view);

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

	private void formatClimateHeating(Climate climate, ClimateView view) {

		if (climate instanceof RoomClimate && ((RoomClimate) climate).getHeating() != null) {
			RoomClimate room = (RoomClimate) climate;
			if (room.getHeating().isBoostActive()) {
				view.setLinkBoost(String.valueOf(room.getHeating().getBoostMinutesLeft()));
			} else {
				view.setLinkBoost("/heatingboost?prefix=" + room.getHeating().getProgramNamePrefix());
			}
			view.setLinkManual("/heatingmanual?prefix=" + room.getHeating().getProgramNamePrefix());
			view.setTargetTemp(format(room.getHeating().getTargetTemperature(), false));
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

		ClimateView viewMin = formatClimate(house.getConclusionClimateFacadeMin(), viewKeyMin);
		ClimateView viewMax = new ClimateView();
		viewMax.setId(viewKeyMax);

		viewMin.setPostfix(house.getConclusionClimateFacadeMin().getPlaceName());

		if (house.getConclusionClimateFacadeMax().getSunBeamIntensity().ordinal() >= house
				.getConclusionClimateFacadeMax().getSunHeatingInContrastToShadeIntensity().ordinal()) {
			viewMax.setStateTemperature(house.getConclusionClimateFacadeMax().getSunBeamIntensity().getSun());
		} else {
			viewMax.setStateTemperature(house.getConclusionClimateFacadeMax()
					.getSunHeatingInContrastToShadeIntensity().getHeating());
		}
		viewMax.setName("Fassade " + house.getConclusionClimateFacadeMax().getPlaceName());

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

	private void formatPower(Model model, PowerMeterModel powerMeter) {

		PowerView power = new PowerView();
		power.setState(powerMeter.getActualConsumption().getValue().intValue() + " Watt");
		power.setName(powerMeter.getDevice().getType());
		power.setIcon("fas fa-bolt");
		if (powerMeter.getActualConsumption().getTendency() != null) {
			power.setTendencyIcon(powerMeter.getActualConsumption().getTendency().getIconCssClass());
		}
		model.addAttribute(powerMeter.getDevice().programNamePrefix(), power);
	}

	private void formatLowBattery(Model model, List<String> lowBatteryDevices) {
		model.addAttribute("lowBattery", lowBatteryDevices);
	}

	private void formatSwitch(Model model, String viewKey, SwitchModel switchModel) {

		SwitchView view = new SwitchView();
		view.setId(viewKey);
		view.setName(switchModel.getDevice().getType());
		view.setState(switchModel.isState() ? "Eingeschaltet" : "Ausgeschaltet");
		if (switchModel.getAutomation() != null) {
			if (switchModel.getAutomation()) {
				view.setState(view.getState() + ", automatisch");
				view.setLinkManual(
						TOGGLE_DEV_ID_VAR + switchModel.getDevice().programNamePrefix() + AUTOMATIC);
			} else {
				view.setState(view.getState() + ", manuell");
				view.setLinkAuto(TOGGLE_DEV_ID_VAR + switchModel.getDevice().programNamePrefix() + AUTOMATIC);
			}
			view.setAutoInfoText(StringUtils.trimToEmpty(switchModel.getAutomationInfoText()));
		}
		view.setLabel(switchModel.isState() ? "ausschalten" : "einschalten");
		view.setIcon(switchModel.isState() ? "fas fa-toggle-on" : "fas fa-toggle-off");
		view.setLink(TOGGLE_DEV_ID_VAR + switchModel.getDevice().accessKeyXmlApi(Datapoint.STATE));
		model.addAttribute(viewKey, view);
	}

	private void formatWindow(Model model, String viewKey, Window windowModel) {

		ShutterView view = new ShutterView();
		view.setId(viewKey);
		view.setName(windowModel.getShutterDevice().getType());
		view.setState(windowModel.getShutterPosition().getText(windowModel.getShutterPositionPercentage()));

		if (windowModel.getShutterAutomation() != null) {
			if (windowModel.getShutterAutomation()) {
				view.setState(view.getState() + ", automatisch");
				view.setLinkManual(
						TOGGLE_DEV_ID_VAR + windowModel.getShutterDevice().programNamePrefix() + AUTOMATIC);
			} else {
				view.setState(view.getState() + ", manuell");
				view.setLinkAuto(
						TOGGLE_DEV_ID_VAR + windowModel.getShutterDevice().programNamePrefix() + AUTOMATIC);
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
			return "/shutterSetPosition?devIdVar="
					+ windowModel.getShutterDevice().accessKeyXmlApi(Datapoint.STATE) + "&shutterSetPosition="
					+ shutterPosition.getControlPosition();
		}
	}

	public void fillLinks(Model model) {
		model.addAttribute("link_internet_ccu2", env.getProperty("link.internet.ccu2"));
		model.addAttribute("link_local_ccu2", env.getProperty("link.local.ccu2"));
		model.addAttribute("link_internet_historian", env.getProperty("link.internet.historian"));
		model.addAttribute("link_local_historian", env.getProperty("link.local.historian"));

	}

}
