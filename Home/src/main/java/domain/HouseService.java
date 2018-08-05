package domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.springframework.ui.Model;

import home.main.HomematicAPI;

public class HouseService {

	private HouseModel house;

	private HomematicAPI api;

	private Map<String, String> viewKeyToDevice;

	public HouseService(HomematicAPI api) {
		this.api = api;
		viewKeyToDevice = new HashMap<>();
		viewKeyToDevice.put("tempBathroom_boost", "Vorbereitung Dusche");
		viewKeyToDevice.put("switchKitchen", "BidCos-RF.OEQ0712456:1");
	}

	public void refreshModel() {

		api.refresh();

		house = new HouseModel();

		house.setBathRoomTemperature(readActTemperature("BidCos-RF.OEQ0854602", "4"));
		house.setBathRoomBoost(readBoost("Vorbereitung Dusche"));

		house.setKidsRoomTemperature(readActTemperature("HmIP-RF.000E97099314A3", "1"));
		house.setKidsRoomHumidity(readHumidity("HmIP-RF.000E97099314A3", "1"));

		house.setLivingRoomTemperature(readActTemperature("?", "?"));
		house.setLivingRoomHumidity(readHumidity("?", "?"));

		house.setBedRoomTemperature(readActTemperature("?", "?"));
		house.setBedRoomHumidity(readHumidity("?", "?"));

		house.setTerraceTemperature(readTemperature("BidCos-RF.OEQ0801741", "2"));
		house.setTerraceSunHeatingDiff(readTemperature("BidCos-RF.OEQ0801741", "3"));

		house.setEntranceTemperature(readTemperature("BidCos-RF.OEQ0801807", "2"));
		house.setEntranceSunHeatingDiff(readTemperature("BidCos-RF.OEQ0801807", "3"));

		house.setKitchenLightSwitchState(readSwitchState("BidCos-RF.OEQ0712456", "1"));

		house.setHouseElectricalPowerConsumption(readPowerConsumption("BidCos-RF.NEQ0861520", "1"));

	}

	public void calculateConclusion() {

		if (house.getTerraceTemperature().compareTo(house.getEntranceTemperature()) < 0) {
			// min
			house.setConclusionFacadeMinTemp(house.getTerraceTemperature());
			house.setConclusionFacadeMinTempName("Terrasse");
			// max
			house.setConclusionFacadeMaxTemp(house.getEntranceTemperature());
			house.setConclusionFacadeMaxTempName("Einfahrt");
			house.setConclusionFacadeMaxTempSunHeating(house.getEntranceSunHeatingDiff());
		} else {
			// min
			house.setConclusionFacadeMinTemp(house.getEntranceTemperature());
			house.setConclusionFacadeMinTempName("Einfahrt");
			// max
			house.setConclusionFacadeMaxTemp(house.getTerraceTemperature());
			house.setConclusionFacadeMaxTempName("Terrasse");
			house.setConclusionFacadeMaxTempSunHeating(house.getTerraceSunHeatingDiff());
		}

		house.setConclusionFacadeSidesDifference(house.getConclusionFacadeMaxTemp().subtract(house.getConclusionFacadeMinTemp()).abs());

		if (house.getConclusionFacadeMaxTempSunHeating().intValue() < 2) {
			house.setConclusionFacadeMaxTempSunIntensity(Intensity.NO);
		} else if (house.getConclusionFacadeMaxTempSunHeating().intValue() < 6) {
			house.setConclusionFacadeMaxTempSunIntensity(Intensity.LOW);
		} else if (house.getConclusionFacadeMaxTempSunHeating().intValue() < 13) {
			house.setConclusionFacadeMaxTempSunIntensity(Intensity.MEDIUM);
		} else {
			house.setConclusionFacadeMaxTempSunIntensity(Intensity.HIGH);
		}

		if (house.getConclusionFacadeSidesDifference().intValue() < 2) {
			house.setConclusionFacadeMaxTempHeatingIntensity(Intensity.NO);
		} else if (house.getConclusionFacadeSidesDifference().intValue() < 6) {
			house.setConclusionFacadeMaxTempHeatingIntensity(Intensity.LOW);
		} else if (house.getConclusionFacadeSidesDifference().intValue() < 13) {
			house.setConclusionFacadeMaxTempHeatingIntensity(Intensity.MEDIUM);
		} else {
			house.setConclusionFacadeMaxTempHeatingIntensity(Intensity.HIGH);
		}
	}

	public void fillViewModel(Model model) {

		formatTemperature(model, "tempBathroom", house.getBathRoomTemperature(), null, house.isBathRoomBoost());
		formatTemperature(model, "tempKids", house.getKidsRoomTemperature(), house.getKidsRoomHumidity(), null);
		formatTemperature(model, "tempLivingroom", house.getLivingRoomTemperature(), house.getLivingRoomHumidity(), null);
		formatTemperature(model, "tempBedroom", house.getBedRoomTemperature(), house.getBedRoomHumidity(), null);

		formatFacadeTemperatures(model, "tempMinHouse", "tempMaxHouse", house);

		formatSwitch(model, "switchKitchen", house.isKitchenLightSwitchState());
		formatPower(model, "powerHouse", house.getHouseElectricalPowerConsumption());
	}

	public void toggle(String key) throws Exception {
		api.toggleBooleanState(viewKeyToDevice.get(key));
	}

	private BigDecimal readTemperature(String device, String chanel) {
		return api.getAsBigDecimal(device + ":" + chanel + ".TEMPERATURE");
	}

	private BigDecimal readActTemperature(String device, String chanel) {
		return api.getAsBigDecimal(device + ":" + chanel + ".ACTUAL_TEMPERATURE");
	}

	private BigDecimal readHumidity(String device, String chanel) {
		return api.getAsBigDecimal(device + ":" + chanel + ".HUMIDITY");
	}

	private boolean readBoost(String var) {
		return api.getAsBoolean(var);
	}

	private boolean readSwitchState(String device, String chanel) {
		return api.getAsBoolean(device + ":" + chanel + ".STATE");
	}

	private int readPowerConsumption(String device, String chanel) {
		return api.getAsBigDecimal(device + ":" + chanel + ".POWER").intValue();
	}

	private String format(BigDecimal val) {
		if (val != null) {
			return new DecimalFormat("0.#").format(val);
		} else {
			return null;
		}
	}

	private void formatTemperature(Model model, String viewKey, BigDecimal temperature, BigDecimal humidity, Boolean boost) {

		String frmt = "";
		String colorClass = "secondary";
		String linkBoost = "";
		if (temperature != null) {
			// Temperature and humidity
			frmt += format(temperature) + " " + "\u00b0" + "C";
			if (humidity != null) {
				frmt += ", " + format(humidity) + " % r.F.";
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
	}

	private void formatFacadeTemperatures(Model model, String viewKeyMin, String viewKeyMax, HouseModel house) {

		model.addAttribute(viewKeyMin + "_postfix", house.getConclusionFacadeMinTempName());
		formatTemperature(model, viewKeyMin, house.getConclusionFacadeMinTemp(), null, null);

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
