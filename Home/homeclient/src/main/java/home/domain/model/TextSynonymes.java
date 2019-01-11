package home.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

import homecontroller.domain.model.Place;
import homecontroller.domain.model.Type;

public class TextSynonymes {

	private static final Map<String, String> SYNONYMES = new LinkedHashMap<>();

	static {
		SYNONYMES.put(Place.KIDSROOM.getPlaceName(), "Bastians Zimmer");
		SYNONYMES.put(Place.HOUSE.getPlaceName(), "Außen");
		SYNONYMES.put(Place.HOUSE.getPlaceName(), "Draußen");
		SYNONYMES.put(Place.TERRACE.getPlaceName(), "Garten");

		SYNONYMES.put(Type.THERMOSTAT.getTypeName(), "Heizkörper");
		SYNONYMES.put(Type.THERMOSTAT.getTypeName(), "Heizung");
		SYNONYMES.put(Type.THERMOMETER.getTypeName(), "Temperatur");
		SYNONYMES.put(Type.THERMOMETER.getTypeName(), "Warm");
		SYNONYMES.put(Type.SUN_SENSOR.getTypeName(), "Sonne");
		SYNONYMES.put(Type.SUN_SENSOR.getTypeName(), "sonnig");
		SYNONYMES.put(Type.SHUTTER_LEFT.getTypeName(), "Rollladen links");
		SYNONYMES.put(Type.SHUTTER_LEFT.getTypeName(), "Rollo links");
		SYNONYMES.put(Type.SHUTTER_RIGHT.getTypeName(), "Rollladen rechts");
		SYNONYMES.put(Type.SHUTTER_RIGHT.getTypeName(), "Rollo rechts");
		SYNONYMES.put(Type.SWITCH_WINDOWLIGHT.getTypeName(), "Licht am Fenster");
		SYNONYMES.put(Type.SWITCH_WINDOWLIGHT.getTypeName(), "Licht");
		SYNONYMES.put(Type.ELECTRIC_POWER.getTypeName(), "Strom");
		SYNONYMES.put(Type.CONCLUSION_OUTSIDE_TEMPERATURE.getTypeName(), "Thermometer");
		SYNONYMES.put(Type.CONCLUSION_OUTSIDE_TEMPERATURE.getTypeName(), "Temperatur");
		SYNONYMES.put(Type.CONCLUSION_OUTSIDE_TEMPERATURE.getTypeName(), "Warm");

	}

	private TextSynonymes() {
		super();
	}

}
