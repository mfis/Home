package home.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.PowerHistoryEntry;
import homecontroller.domain.model.Climate;
import homecontroller.domain.model.Datapoint;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Intensity;
import homecontroller.domain.model.PowerConsumptionMonth;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.SwitchModel;

@Component
public class HouseView {

	private final static BigDecimal HIGH_TEMP = new BigDecimal("25");
	private final static BigDecimal LOW_TEMP = new BigDecimal("19");
	private final static BigDecimal FROST_TEMP = new BigDecimal("3");

	@Autowired
	private Environment env;

	public void fillViewModel(Model model, HouseModel house) {

		formatClimate(model, "tempBathroom", house.getClimateBathRoom());
		formatClimate(model, "tempKids", house.getClimateKidsRoom());
		formatClimate(model, "tempLivingroom", house.getClimateLivingRoom());
		formatClimate(model, "tempBedroom", house.getClimateBedRoom());

		formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

		formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());
		formatPower(model, "powerHouse", house.getHouseElectricalPowerConsumption());

		formatLowBattery(model, house.getLowBatteryDevices());
	}

	public void fillHistoryViewModel(Model model, HistoryModel history) {

		List<PowerHistoryEntry> list = new LinkedList<>();
		PowerHistoryEntry entry = null;
		Calendar cal = null;
		int count = 0;
		for (PowerConsumptionMonth pcm : history.getElectricPowerConsumption().values()) {
			cal = new GregorianCalendar();
			cal.setTimeInMillis(pcm.getMeasurePointMax());
			entry = new PowerHistoryEntry();
			entry.setKey(new SimpleDateFormat("MMM yyyy", Locale.GERMANY).format(cal.getTime()));
			entry.setValue(new DecimalFormat("0").format(pcm.getPowerConsumption()) + " kW/h");
			if (count < history.getElectricPowerConsumption().size() - 3) {
				entry.setCollapse(" collapse multi-collapse electricity");
			}
			if (count == history.getElectricPowerConsumption().size() - 1) {
				if (cal.get(Calendar.DAY_OF_MONTH) > 1) {
					YearMonth yearMonthObject = YearMonth.of(cal.get(Calendar.YEAR),
							cal.get(Calendar.MONTH) + 1);
					int daysInMonth = yearMonthObject.lengthOfMonth();
					int hoursAgo = ((cal.get(Calendar.DAY_OF_MONTH) - 1) * 24)
							+ cal.get(Calendar.HOUR_OF_DAY);
					int hoursToGo = (daysInMonth * 24) - hoursAgo;
					if (hoursAgo > 0) {
						BigDecimal actualValue = new BigDecimal(pcm.getPowerConsumption());
						BigDecimal calculated = actualValue
								.add(actualValue.divide(new BigDecimal(hoursAgo), 2, RoundingMode.HALF_UP)
										.multiply(new BigDecimal(hoursToGo)));
						entry.setCalculated(new DecimalFormat("0").format(calculated) + " kW/h");
					}
				}
				entry.setColorClass(" list-group-item-secondary");
				entry.setKey(entry.getKey() + " bisher");
			}
			list.add(entry);
			count++;
		}

		Collections.reverse(list);
		model.addAttribute("power", list);
	}

	private String format(BigDecimal val, boolean rounded) {
		if (val != null) {
			return new DecimalFormat("0." + (rounded ? "#" : "0")).format(val);
		} else {
			return null;
		}
	}

	private void formatClimate(Model model, String viewKey, Climate climate) {

		String frmt = "";
		String colorClass = "secondary";
		String linkBoost = "";
		String linkManual = "";
		String targetTemp = "";
		String icon = "";
		String heatericon = "";

		if (climate.getTemperature() != null && climate.getTemperature().compareTo(BigDecimal.ZERO) == 0
				&& climate.getHumidity() != null && climate.getHumidity().compareTo(BigDecimal.ZERO) == 0) {
			frmt = "unbekannt";
		}

		if (climate.getTemperature() != null) {
			// Temperature and humidity
			frmt += format(climate.getTemperature(), false) + "\u00b0" + "C";
			if (climate.getHumidity() != null) {
				frmt += ", " + format(climate.getHumidity(), true) + "%rH";
			}
			// Background color
			if (climate.getTemperature().compareTo(HIGH_TEMP) > 0) {
				colorClass = "danger";
				icon = "fas fa-thermometer-full";
			} else if (climate.getTemperature().compareTo(FROST_TEMP) < 0) {
				colorClass = "info";
				icon = "far fa-snowflake";
			} else if (climate.getTemperature().compareTo(LOW_TEMP) < 0) {
				colorClass = "info";
				icon = "fas fa-thermometer-empty";
			} else {
				colorClass = "success";
				icon = "fas fa-thermometer-half";
			}
			// Heating
			if (climate instanceof RoomClimate && ((RoomClimate) climate).getHeating() != null) {
				RoomClimate room = (RoomClimate) climate;
				if (room.getHeating().isBoostActive()) {
					linkBoost = String.valueOf(room.getHeating().getBoostMinutesLeft());
				} else {
					linkBoost = "/heatingboost?prefix=" + room.getHeating().getProgramNamePrefix();
				}
				linkManual = "/heatingmanual?prefix=" + room.getHeating().getProgramNamePrefix();
				targetTemp = format(room.getHeating().getTargetTemperature(), false);
				heatericon = "fab fa-hotjar";
			}
		} else {
			frmt += "?";
		}

		model.addAttribute(viewKey, frmt);
		model.addAttribute(viewKey + "_colorClass", colorClass);
		model.addAttribute(viewKey + "_icon", icon);
		model.addAttribute(viewKey + "_heatericon", heatericon);
		model.addAttribute(viewKey + "_linkBoost", linkBoost);
		model.addAttribute(viewKey + "_linkManual", linkManual);
		model.addAttribute(viewKey + "_targetTemp", targetTemp);
		if (climate instanceof RoomClimate && ((RoomClimate) climate).getHint() != null) {
			model.addAttribute(viewKey + "_hint",
					StringUtils.trimToEmpty(((RoomClimate) climate).getHint().getText()));
		} else {
			model.addAttribute(viewKey + "_hint", null);
		}
	}

	private void formatFacadeTemperatures(Model model, String viewKeyMin, String viewKeyMax,
			HouseModel house) {

		model.addAttribute(viewKeyMin + "_postfix", house.getConclusionClimateFacadeMin().getPlaceName());
		formatClimate(model, viewKeyMin, house.getConclusionClimateFacadeMin());

		if (house.getConclusionClimateFacadeMax().getSunBeamIntensity().ordinal() >= house
				.getConclusionClimateFacadeMax().getSunHeatingInContrastToShadeIntensity().ordinal()) {
			model.addAttribute(viewKeyMax,
					house.getConclusionClimateFacadeMax().getSunBeamIntensity().getSun());
		} else {
			model.addAttribute(viewKeyMax, house.getConclusionClimateFacadeMax()
					.getSunHeatingInContrastToShadeIntensity().getHeating());
		}
		model.addAttribute(viewKeyMax + "_name",
				"Fassade " + house.getConclusionClimateFacadeMax().getPlaceName());

		switch (Intensity.max(house.getConclusionClimateFacadeMax().getSunBeamIntensity(),
				house.getConclusionClimateFacadeMax().getSunHeatingInContrastToShadeIntensity())) {
		case NO:
			model.addAttribute(viewKeyMin + "_postfix", ""); // No sun, no
																// heating -> no
																// special house
																// side name
			model.addAttribute(viewKeyMax + "_colorClass", "secondary");
			break;
		case LOW:
			model.addAttribute(viewKeyMax + "_colorClass", "dark");
			model.addAttribute(viewKeyMax + "_icon", "far fa-sun");
			break;
		case MEDIUM:
			model.addAttribute(viewKeyMax + "_colorClass", "warning");
			model.addAttribute(viewKeyMax + "_icon", "far fa-sun");
			break;
		case HIGH:
			model.addAttribute(viewKeyMax + "_colorClass", "danger");
			model.addAttribute(viewKeyMax + "_icon", "fas fa-sun");
		}

	}

	private void formatPower(Model model, String viewKey, Integer consumption) {

		String frmt = "";
		if (consumption != null) {
			frmt += consumption + " Watt";
		} else {
			frmt += "?";
		}
		model.addAttribute(viewKey, frmt);
		model.addAttribute(viewKey + "_icon", "fas fa-bolt");
	}

	private void formatLowBattery(Model model, List<String> lowBatteryDevices) {
		model.addAttribute("lowBattery", lowBatteryDevices);
	}

	private void formatSwitch(Model model, String viewKey, SwitchModel switchModel) {

		String frmt = "";
		String label = "";
		String link = "#";
		String linkAuto = "#";
		String linkManual = "#";
		String autoInfoText = "";
		String icon = "";
		if (switchModel != null) {
			frmt += (switchModel.isState() ? "Eingeschaltet" : "Ausgeschaltet");
			if (switchModel.getAutomation() != null) {
				if (switchModel.getAutomation() == true) {
					frmt += ", automatisch";
					linkManual = "/toggle?devIdVar=" + switchModel.getDevice().programNamePrefix()
							+ "Automatic";
				} else {
					frmt += ", manuell";
					linkAuto = "/toggle?devIdVar=" + switchModel.getDevice().programNamePrefix()
							+ "Automatic";
				}
				autoInfoText = StringUtils.trimToEmpty(switchModel.getAutomationInfoText());
			}
			label += (switchModel.isState() ? "ausschalten" : "einschalten");
			icon += (switchModel.isState() ? "fas fa-toggle-on" : "fas fa-toggle-off");
			link = "/toggle?devIdVar=" + switchModel.getDevice().accessKeyXmlApi(Datapoint.STATE);
		} else {
			frmt += "?";
		}
		model.addAttribute(viewKey, frmt);
		model.addAttribute(viewKey + "_label", label);
		model.addAttribute(viewKey + "_link", link);
		model.addAttribute(viewKey + "_linkAuto", linkAuto);
		model.addAttribute(viewKey + "_linkManual", linkManual);
		model.addAttribute(viewKey + "_autoInfoText", autoInfoText);
		model.addAttribute(viewKey + "_icon", icon);
	}

	public void fillLinks(Model model) {
		model.addAttribute("link_internet_ccu2", env.getProperty("link.internet.ccu2"));
		model.addAttribute("link_local_ccu2", env.getProperty("link.local.ccu2"));
		model.addAttribute("link_internet_historian", env.getProperty("link.internet.historian"));
		model.addAttribute("link_local_historian", env.getProperty("link.local.historian"));

	}

}
