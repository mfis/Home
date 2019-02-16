package home.domain.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import homecontroller.domain.model.Place;
import homecontroller.domain.model.ShutterPosition;
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
		SYNONYMES.add(new Synonym<Place>("Küchen", Place.KITCHEN));
		SYNONYMES.add(new Synonym<Place>("Außen|Draußen|Garten", Place.OUTSIDE));

		SYNONYMES.add(new Synonym<Type>("Heizkörper|Heizung", Type.THERMOSTAT));
		SYNONYMES.add(new Synonym<Type>("Temperatur|warm|kalt|Wärme|Kälte|Grad|Wetter|Luftfeuchtigkeit",
				Type.THERMOMETER));
		SYNONYMES.add(new Synonym<Type>("Sonne|sonnig|Schatten|schatting", Type.SUN_SENSOR));
		SYNONYMES.add(new Synonym<Type>("sonnig", Type.SUN_SENSOR));
		SYNONYMES.add(new Synonym<Type>("Rollade|Rollladen|Rollo|Rollos (links|linke|linken|linkes)",
				Type.SHUTTER_LEFT));
		SYNONYMES.add(new Synonym<Type>("Rollade|Rollladen|Rollo|Rollos (rechts|rechte|rechtes|rechten)",
				Type.SHUTTER_RIGHT));
		SYNONYMES.add(new Synonym<Type>("Licht|Lampe|Leuchte (Fenster)", Type.SWITCH_WINDOWLIGHT));
		SYNONYMES.add(new Synonym<Type>("Strom", Type.ELECTRIC_POWER));
		SYNONYMES.add(new Synonym<Type>(
				"Thermometer|Temperatur|warm|kalt|Wärme|Kälte|Grad|Wetter|sonnig|Sonne|Luftfeuchtigkeit",
				Type.CONCLUSION_OUTSIDE_TEMPERATURE));
		
		SYNONYMES.add(new Synonym<ShutterPosition>("öffne|hoch|oben", ShutterPosition.OPEN));
		SYNONYMES.add(new Synonym<ShutterPosition>("halb|halbe|Hälfte", ShutterPosition.HALF));
		SYNONYMES.add(new Synonym<ShutterPosition>("Sonnenschutz|Sonne|Schatten|Beschattung", ShutterPosition.SUNSHADE));
		SYNONYMES.add(new Synonym<ShutterPosition>("schließe|schliesse|runter|herunter|unten", ShutterPosition.CLOSE));

		SYNONYMES.add(new Synonym<Boolean>("ein|an|öffne", Boolean.TRUE));
		SYNONYMES.add(new Synonym<Boolean>("aus|schließe|schliesse", Boolean.FALSE));
		
		SYNONYMES.add(new Synonym<Integer>("ein|eins", 1));
		SYNONYMES.add(new Synonym<Integer>("zwei", 2));
		SYNONYMES.add(new Synonym<Integer>("drei", 3));
		SYNONYMES.add(new Synonym<Integer>("vier", 4));
		SYNONYMES.add(new Synonym<Integer>("fünf", 5));
		SYNONYMES.add(new Synonym<Integer>("sechs", 6));
		SYNONYMES.add(new Synonym<Integer>("sieben", 7));
		SYNONYMES.add(new Synonym<Integer>("acht", 8));
		SYNONYMES.add(new Synonym<Integer>("neun", 9));
		SYNONYMES.add(new Synonym<Integer>("zehn", 10));
		
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

	public static List<Synonym<?>> getAllSynonymes() { // NOSONAR
		return Collections.unmodifiableList(SYNONYMES);
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
