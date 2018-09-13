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
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import home.domain.model.PowerHistoryEntry;
import homecontroller.domain.model.HeatingModel;
import homecontroller.domain.model.HistoryModel;
import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.Intensity;
import homecontroller.domain.model.SwitchModel;

@Component
public class HouseView {

	public void fillViewModel(Model model, HouseModel house) {

		formatTemperature(model, "tempBathroom", house.getBathRoomTemperature(), null, house.getBathRoomHeating(), house.getConclusionHintBathRoom());
		formatTemperature(model, "tempKids", house.getKidsRoomTemperature(), house.getKidsRoomHumidity(), null, house.getConclusionHintKidsRoom());
		formatTemperature(model, "tempLivingroom", house.getLivingRoomTemperature(), house.getLivingRoomHumidity(), null, house.getConclusionHintLivingRoom());
		formatTemperature(model, "tempBedroom", house.getBedRoomTemperature(), house.getBedRoomHumidity(), null, house.getConclusionHintBedRoom());

		formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

		formatSwitch(model, "switchKitchen", house.getKitchenWindowLightSwitch());
		formatPower(model, "powerHouse", house.getHouseElectricalPowerConsumption());
	}

	public void fillHistoryViewModel(Model model, HistoryModel history) {

		List<PowerHistoryEntry> list = new LinkedList<>();
		PowerHistoryEntry entry = null;
		Calendar cal = null;
		BigDecimal value = null;
		BigDecimal lastValue = null;
		int count = 0;
		for (long key : history.getMonthlyPowerConsumption().keySet()) {
			if (lastValue != null) {
				cal = new GregorianCalendar();
				cal.setTimeInMillis(key);
				value = history.getMonthlyPowerConsumption().get(key).subtract(lastValue).divide(new BigDecimal(1000));
				entry = new PowerHistoryEntry();
				entry.setKey(new SimpleDateFormat("MMM yyyy", Locale.GERMANY).format(cal.getTime()));
				entry.setValue(new DecimalFormat("0").format(value) + " kW/h");
				if (count < history.getMonthlyPowerConsumption().size() - 3) {
					entry.setCollapse(" collapse multi-collapse electricity");
				}
				list.add(entry);
			}
			lastValue = history.getMonthlyPowerConsumption().get(key);
			count++;
		}

		if (cal.get(Calendar.DAY_OF_MONTH) > 1) {
			YearMonth yearMonthObject = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
			int daysInMonth = yearMonthObject.lengthOfMonth();
			int hoursAgo = ((cal.get(Calendar.DAY_OF_MONTH) - 1) * 24) + cal.get(Calendar.HOUR_OF_DAY);
			int hoursToGo = (daysInMonth * 24) - hoursAgo;
			if (hoursAgo > 0) {
				BigDecimal calculated = value.add(value.divide(new BigDecimal(hoursAgo), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(hoursToGo)));
				entry.setCalculated(new DecimalFormat("0").format(calculated) + " kW/h");
			}
		}

		entry.setColorClass(" list-group-item-secondary");
		entry.setKey(entry.getKey() + " bisher");

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

	private void formatTemperature(Model model, String viewKey, BigDecimal temperature, BigDecimal humidity, HeatingModel heating, String hint) {

		String frmt = "";
		String colorClass = "secondary";
		String linkBoost = "";
		String linkManual = "";
		String targetTemp = "";

		if (temperature != null && temperature.compareTo(BigDecimal.ZERO) == 0 && humidity != null && humidity.compareTo(BigDecimal.ZERO) == 0) {
			frmt = "unbekannt";
		}

		if (temperature != null) {
			// Temperature and humidity
			frmt += format(temperature, false) + "\u00b0" + "C";
			if (humidity != null) {
				frmt += ", " + format(humidity, true) + "%rF";
			}
			// Background color
			if (temperature.compareTo(new BigDecimal("25")) > 0) {
				colorClass = "danger";
			} else if (temperature.compareTo(new BigDecimal("19")) < 0) {
				colorClass = "info";
			} else {
				colorClass = "success";
			}
			// Heating
			if (heating != null) {
				if (heating.isBoostActive()) {
					linkBoost = String.valueOf(heating.getBoostMinutesLeft());
				} else {
					linkBoost = "/heatingboost?prefix=" + heating.getProgramNamePrefix();
				}
				linkManual = "/heatingmanual?prefix=" + heating.getProgramNamePrefix();
				targetTemp = format(heating.getTargetTemperature(), false);
			}
		} else {
			frmt += "?";
		}

		model.addAttribute(viewKey, frmt);
		model.addAttribute(viewKey + "_colorClass", colorClass);
		model.addAttribute(viewKey + "_linkBoost", linkBoost);
		model.addAttribute(viewKey + "_linkManual", linkManual);
		model.addAttribute(viewKey + "_targetTemp", targetTemp);
		model.addAttribute(viewKey + "_hint", StringUtils.trimToEmpty(hint));
	}

	private void formatFacadeTemperatures(Model model, String viewKeyMin, String viewKeyMax, HouseModel house) {

		model.addAttribute(viewKeyMin + "_postfix", house.getConclusionFacadeMinTempName());
		formatTemperature(model, viewKeyMin, house.getConclusionFacadeMinTemp(), null, null, null);

		if (house.getConclusionFacadeMaxTempSunIntensity().ordinal() >= house.getConclusionFacadeMaxTempHeatingIntensity().ordinal()) {
			model.addAttribute(viewKeyMax, house.getConclusionFacadeMaxTempSunIntensity().getSun());
		} else {
			model.addAttribute(viewKeyMax, house.getConclusionFacadeMaxTempHeatingIntensity().getHeating());
		}
		model.addAttribute(viewKeyMax + "_name", "Fassade " + house.getConclusionFacadeMaxTempName());

		switch (Intensity.max(house.getConclusionFacadeMaxTempSunIntensity(), house.getConclusionFacadeMaxTempHeatingIntensity())) {
		case NO:
			model.addAttribute(viewKeyMin + "_postfix", ""); // No sun, no
																// heating -> no
																// special house
																// side name
			model.addAttribute(viewKeyMax + "_colorClass", "secondary");
			break;
		case LOW:
			model.addAttribute(viewKeyMax + "_colorClass", "dark");
			break;
		case MEDIUM:
			model.addAttribute(viewKeyMax + "_colorClass", "warning");
			break;
		case HIGH:
			model.addAttribute(viewKeyMax + "_colorClass", "danger");
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
	}

	private void formatSwitch(Model model, String viewKey, SwitchModel switchModel) {

		String frmt = "";
		String label = "";
		String link = "#";
		if (switchModel != null) {
			frmt += (switchModel.isState() ? "Eingeschaltet" : "Ausgeschaltet");
			label += (switchModel.isState() ? "ausschalten" : "einschalten");
			link = "window.location.href = '/toggle?devIdVar=" + switchModel.getDeviceIdVar() + "'";
		} else {
			frmt += "?";
		}
		model.addAttribute(viewKey, frmt);
		model.addAttribute(viewKey + "_label", label);
		model.addAttribute(viewKey + "_link", link);
	}

}
