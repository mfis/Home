package home.main;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

	@Value("${spring.application.name}")
	String appName;

	@Autowired
	private Environment env;

	private static HomematicAPI api;

	@PostConstruct
	public void init() {
		String hmHost = env.getProperty("homematic.hostName");
		String hmDevPrefixes = env.getProperty("homematic.devicePrefixes");
		api = new HomematicAPI(hmHost, new ArrayList<String>(Arrays.asList(hmDevPrefixes.split(","))));
	}

	@RequestMapping("/toggle")
	public String toggle(@RequestParam("key") String key) throws Exception {
		api.toggleBooleanState(valueFromKey(key));
		return "redirect:/";
	}

	@RequestMapping("/")
	public String homePage(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		api.refresh();

		formatTemperature(model, "tempBathroom");
		formatTemperature(model, "tempKids");
		formatTemperature(model, "tempLivingroom");
		formatTemperature(model, "tempBedroom");
		formatTemperaturesOutside("tempMinHouse", model, "tempEntrance", "tempTerrace");
		formatSunOutside("tempDiffHouse", model, "tempDiffEntrance", "Einfahrt", "tempDiffTerrace", "Terrasse");
		formatSwitch(model, "switchKitchen");
		formatPower(model, "powerHouse");

		return "home";
	}

	private String valueFromKey(String key) {
		String val = env.getProperty(key);
		if (val != null) {
			val = val.trim();
		}
		return val;
	}

	private void formatTemperature(Model model, String key) {

		String device = valueFromKey(key);

		String temperature = api.getAsFormattedBigDecimal(device + ".ACTUAL_TEMPERATURE");
		String humidity = api.getAsString(device + ".HUMIDITY");
		String frmt = "";
		String colorClass = "secondary";
		String linkBoost = "";
		if (temperature != null) {
			// Temperature and humidity
			frmt += temperature + " " + "\u00b0" + "C";
			if (humidity != null) {
				frmt += ", " + humidity + " % r.F.";
			}
			// Background color
			BigDecimal bd = api.getAsBigDecimal(device + ".ACTUAL_TEMPERATURE");
			if (bd.compareTo(new BigDecimal("25")) > 0) {
				colorClass = "danger";
			} else if (bd.compareTo(new BigDecimal("19")) < 0) {
				colorClass = "info";
			} else {
				colorClass = "success";
			}
			// Boost
			String deviceBoost = valueFromKey(key + "_boost");
			if (deviceBoost != null) {
				if (api.getAsBoolean(deviceBoost)) {
					linkBoost = "#";
				} else {
					linkBoost = "window.location.href = '/toggle?key=" + key + "_boost'";
				}
			}
		} else {
			frmt += "?";
		}
		model.addAttribute(key, frmt);
		model.addAttribute(key + "_colorClass", colorClass);
		model.addAttribute(key + "_linkBoost", linkBoost);
	}

	private void formatTemperaturesOutside(String target, Model model, String keyA, String keyB) {

		String deviceA = valueFromKey(keyA);
		String deviceB = valueFromKey(keyB);

		BigDecimal bdA = api.getAsBigDecimal(deviceA + ".TEMPERATURE");
		BigDecimal bdB = api.getAsBigDecimal(deviceB + ".TEMPERATURE");

		BigDecimal min;
		String minString;
		if (bdA.compareTo(bdB) < 0) {
			min = bdA;
			minString = api.getAsFormattedBigDecimal(deviceA + ".TEMPERATURE");
		} else {
			min = bdB;
			minString = api.getAsFormattedBigDecimal(deviceB + ".TEMPERATURE");
		}
		model.addAttribute(target, minString + " " + "\u00b0" + "C");

		String colorClass = "secondary";
		// Background color
		if (min.compareTo(new BigDecimal("25")) > 0) {
			colorClass = "danger";
		} else if (min.compareTo(new BigDecimal("19")) < 0) {
			colorClass = "info";
		} else {
			colorClass = "success";
		}

		model.addAttribute(target + "_colorClass", colorClass);
	}

	private void formatSunOutside(String target, Model model, String keyA, String nameA, String keyB, String nameB) {

		String deviceA = valueFromKey(keyA);
		String deviceB = valueFromKey(keyB);

		int a = api.getAsBigDecimal(deviceA + ".TEMPERATURE").intValue();
		int b = api.getAsBigDecimal(deviceB + ".TEMPERATURE").intValue();

		int max;
		String maxName;
		if (a > b) {
			max = a;
			maxName = nameA;
		} else {
			max = b;
			maxName = nameB;
		}

		if (max >= Integer.parseInt(env.getProperty("diffTempSun"))) {
			model.addAttribute(target, maxName + ": " + env.getProperty("nameSun"));
			model.addAttribute(target + "_colorClass", "danger");
		} else if (max >= Integer.parseInt(env.getProperty("diffTempLightSun"))) {
			model.addAttribute(target, maxName + ": " + env.getProperty("nameLightSun"));
			model.addAttribute(target + "_colorClass", "warning");
		} else {
			model.addAttribute(target, env.getProperty("nameNoSun"));
			model.addAttribute(target + "_colorClass", "secondary");
		}
	}

	private void formatPower(Model model, String key) {

		String device = valueFromKey(key);

		String power = api.getAsFormattedBigDecimal(device + ".POWER");
		String frmt = "";
		if (power != null) {
			frmt += power + " Watt";
		} else {
			frmt += "?";
		}
		model.addAttribute(key, frmt);
	}

	private void formatSwitch(Model model, String key) {

		String device = valueFromKey(key);

		Boolean state = api.getAsBoolean(device + ".STATE");
		String frmt = "";
		String label = "";
		String link = "#";
		if (state != null) {
			frmt += (state ? "Eingeschaltet" : "Ausgeschaltet");
			label += (state ? "ausschalten" : "einschalten");
			link = "window.location.href = '/toggle?key=" + key + "'";
		} else {
			frmt += "?";
		}
		model.addAttribute(key, frmt);
		model.addAttribute(key + "_label", label);
		model.addAttribute(key + "_link", link);
	}

}