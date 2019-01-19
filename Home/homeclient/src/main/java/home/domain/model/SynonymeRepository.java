package home.domain.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import homecontroller.domain.model.Place;
import homecontroller.domain.model.Type;

public class SynonymeRepository {

	private static final List<String> CONTROL_SYNONYMES = new LinkedList<>();
	private static final List<Synonym<?>> SYNONYMES = new LinkedList<>();

	static {

		for (Place place : Place.values()) {
			SYNONYMES.add(new Synonym<Place>(place.getPlaceName(), place));
		}

		for (Type type : Type.values()) {
			SYNONYMES.add(new Synonym<Type>(type.getTypeName(), type));
		}

		SYNONYMES.add(new Synonym<Place>("Bastian|Bastians", Place.KIDSROOM));
		SYNONYMES.add(new Synonym<Place>("Badezimmer", Place.BATHROOM));
		SYNONYMES.add(new Synonym<Place>("Außen|Draußen|Garten", Place.OUTSIDE));

		SYNONYMES.add(new Synonym<Type>("Heizkörper|Heizung", Type.THERMOSTAT));
		SYNONYMES.add(new Synonym<Type>("Temperatur|warm|kalt|Wetter", Type.THERMOMETER));
		SYNONYMES.add(new Synonym<Type>("Sonne|sonnig|Schatten|schatting", Type.SUN_SENSOR));
		SYNONYMES.add(new Synonym<Type>("sonnig", Type.SUN_SENSOR));
		SYNONYMES.add(new Synonym<Type>("Rollade|Rollladen|Rollo|Rollos (links|linke|linken|linkes)",
				Type.SHUTTER_LEFT));
		SYNONYMES.add(new Synonym<Type>("Rollade|Rollladen|Rollo|Rollos (rechts|rechte|rechtes|rechten)",
				Type.SHUTTER_RIGHT));
		SYNONYMES.add(new Synonym<Type>("Licht|Lampe|Leuchte (Fenster)", Type.SWITCH_WINDOWLIGHT));
		SYNONYMES.add(new Synonym<Type>("Strom", Type.ELECTRIC_POWER));
		SYNONYMES.add(new Synonym<Type>("Thermometer|Temperatur|warm|kalt|Wetter",
				Type.CONCLUSION_OUTSIDE_TEMPERATURE));

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

	private SynonymeRepository() {
		super();
	}

	public static List<String> getControlSynonymes() {
		return Collections.unmodifiableList(CONTROL_SYNONYMES);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<Synonym<T>> getSynonymes(Class<T> t) {
		List<Synonym<T>> x = new LinkedList<>();
		for (Synonym<?> e : SYNONYMES) {
			if (e.getBase().getClass().equals(t)) {
				x.add((Synonym<T>) e);
			}
		}
		return x;
	}

}
