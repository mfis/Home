package home.domain.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import homecontroller.domain.model.Place;
import homecontroller.domain.model.Type;

public class TextSynonymes {

	private static final List<Synonym<Place>> PLACE_SYNONYMES = new LinkedList<>();
	private static final List<Synonym<Type>> TYPE_SYNONYMES = new LinkedList<>();
	private static final List<String> CONTROL_SYNONYMES = new LinkedList<>();

	static {
		PLACE_SYNONYMES.add(new Synonym<Place>("Bastian", Place.KIDSROOM));
		PLACE_SYNONYMES.add(new Synonym<Place>("Bastians", Place.KIDSROOM));

		PLACE_SYNONYMES.add(new Synonym<Place>("Badezimmer", Place.BATHROOM));

		PLACE_SYNONYMES.add(new Synonym<Place>("Außen", Place.OUTSIDE));
		PLACE_SYNONYMES.add(new Synonym<Place>("Draußen", Place.OUTSIDE));
		PLACE_SYNONYMES.add(new Synonym<Place>("Garten", Place.TERRACE));

		TYPE_SYNONYMES.add(new Synonym<Type>("Heizkörper", Type.THERMOSTAT));
		TYPE_SYNONYMES.add(new Synonym<Type>("Heizung", Type.THERMOSTAT));

		TYPE_SYNONYMES.add(new Synonym<Type>("Temperatur", Type.THERMOMETER));
		TYPE_SYNONYMES.add(new Synonym<Type>("Warm", Type.THERMOMETER));
		TYPE_SYNONYMES.add(new Synonym<Type>("Kalt", Type.THERMOMETER));
		TYPE_SYNONYMES.add(new Synonym<Type>("Wetter", Type.THERMOMETER));

		TYPE_SYNONYMES.add(new Synonym<Type>("Sonne", Type.SUN_SENSOR));
		TYPE_SYNONYMES.add(new Synonym<Type>("sonnig", Type.SUN_SENSOR));

		TYPE_SYNONYMES.add(new Synonym<Type>("Rollladen links", Type.SHUTTER_LEFT));
		TYPE_SYNONYMES.add(new Synonym<Type>("Rollo links", Type.SHUTTER_LEFT));

		TYPE_SYNONYMES.add(new Synonym<Type>("Rollladen rechts", Type.SHUTTER_RIGHT));
		TYPE_SYNONYMES.add(new Synonym<Type>("Rollo rechts", Type.SHUTTER_RIGHT));

		TYPE_SYNONYMES.add(new Synonym<Type>("Licht am Fenster", Type.SWITCH_WINDOWLIGHT));
		TYPE_SYNONYMES.add(new Synonym<Type>("Licht", Type.SWITCH_WINDOWLIGHT));

		TYPE_SYNONYMES.add(new Synonym<Type>("Strom", Type.ELECTRIC_POWER));

		TYPE_SYNONYMES.add(new Synonym<Type>("Thermometer", Type.CONCLUSION_OUTSIDE_TEMPERATURE));
		TYPE_SYNONYMES.add(new Synonym<Type>("Temperatur", Type.CONCLUSION_OUTSIDE_TEMPERATURE));
		TYPE_SYNONYMES.add(new Synonym<Type>("Warm", Type.CONCLUSION_OUTSIDE_TEMPERATURE));

		CONTROL_SYNONYMES.add("Setze");
		CONTROL_SYNONYMES.add("setzen");
		CONTROL_SYNONYMES.add("Stelle");
		CONTROL_SYNONYMES.add("stellen");
		CONTROL_SYNONYMES.add("Schalte");
		CONTROL_SYNONYMES.add("schalten");
		CONTROL_SYNONYMES.add("Öffne");
		CONTROL_SYNONYMES.add("öffnen");
		CONTROL_SYNONYMES.add("Schließe");
		CONTROL_SYNONYMES.add("schließen");
		CONTROL_SYNONYMES.add("Steuere");
		CONTROL_SYNONYMES.add("steuern");
		CONTROL_SYNONYMES.add("Regle");
		CONTROL_SYNONYMES.add("reglen");

	}

	private TextSynonymes() {
		super();
	}

	public static List<Synonym<Place>> getPlaceSynonymes() {
		return Collections.unmodifiableList(PLACE_SYNONYMES);
	}

	public static List<Synonym<Type>> getTypeSynonymes() {
		return Collections.unmodifiableList(TYPE_SYNONYMES);
	}

	public static List<String> getControlSynonymes() {
		return Collections.unmodifiableList(CONTROL_SYNONYMES);
	}
}
