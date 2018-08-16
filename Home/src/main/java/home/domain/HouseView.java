package home.domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class HouseView {

	public void fillViewModel(Model model, HouseModel house) {

		formatTemperature(model, "tempBathroom", house.getBathRoomTemperature(), null, house.isBathRoomBoost(), house.getConclusionHintBathRoom());
		formatTemperature(model, "tempKids", house.getKidsRoomTemperature(), house.getKidsRoomHumidity(), null, house.getConclusionHintKidsRoom());
		formatTemperature(model, "tempLivingroom", house.getLivingRoomTemperature(), house.getLivingRoomHumidity(), null, house.getConclusionHintLivingRoom());
		formatTemperature(model, "tempBedroom", house.getBedRoomTemperature(), house.getBedRoomHumidity(), null, house.getConclusionHintBedRoom());

		formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

		formatSwitch(model, "switchKitchen", house.isKitchenLightSwitchState());
		formatPower(model, "powerHouse", house.getHouseElectricalPowerConsumption());
	}

	private String format(BigDecimal val) {
		if (val != null) {
			return new DecimalFormat("0.#").format(val);
		} else {
			return null;
		}
	}

	private void formatTemperature(Model model, String viewKey, BigDecimal temperature, BigDecimal humidity, Boolean boost, String hint) {

		String frmt = "";
		String colorClass = "secondary";
		String linkBoost = "";
		if (temperature != null) {
			// Temperature and humidity
			frmt += format(temperature) + "\u00b0" + "C";
			if (humidity != null) {
				frmt += ", " + format(humidity) + "%rF";
			}
			// Background color
			if (temperature.compareTo(new BigDecimal("25")) > 0) {
				colorClass = "danger";
			} else if (temperature.compareTo(new BigDecimal("19")) < 0) {
				colorClass = "info";
			} else {
				colorClass = "success";
			}
			// Boost
			if (boost != null) {
				if (boost) {
					linkBoost = "#";
				} else {
					linkBoost = "window.location.href = '/toggle?key=" + viewKey + "_boost'";
				}
			}
		} else {
			frmt += "?";
		}

		model.addAttribute(viewKey, frmt);
		model.addAttribute(viewKey + "_colorClass", colorClass);
		model.addAttribute(viewKey + "_linkBoost", linkBoost);
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

	private void formatSwitch(Model model, String viewKey, Boolean state) {

		String frmt = "";
		String label = "";
		String link = "#";
		if (state != null) {
			frmt += (state ? "Eingeschaltet" : "Ausgeschaltet");
			label += (state ? "ausschalten" : "einschalten");
			link = "window.location.href = '/toggle?key=" + viewKey + "'";
		} else {
			frmt += "?";
		}
		model.addAttribute(viewKey, frmt);
		model.addAttribute(viewKey + "_label", label);
		model.addAttribute(viewKey + "_link", link);
	}

}
