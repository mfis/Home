package de.fimatas.home.client.domain.model;

import java.util.EnumMap;

import de.fimatas.home.library.domain.model.Place;

public class PlacePrepositions {

	private static final String FALLBACK_PREPOSITION = "Am Ort";

	private static final EnumMap<Place, String> PLACE_PREPOSITIONS = new EnumMap<>(Place.class);

	static {
		PLACE_PREPOSITIONS.put(Place.KITCHEN, "In der");
		PLACE_PREPOSITIONS.put(Place.BATHROOM, "Im");
		PLACE_PREPOSITIONS.put(Place.BEDROOM, "Im");
		PLACE_PREPOSITIONS.put(Place.ENTRANCE, "Auf der");
		PLACE_PREPOSITIONS.put(Place.HOUSE, "Im");
		PLACE_PREPOSITIONS.put(Place.KIDSROOM, "Im");
		PLACE_PREPOSITIONS.put(Place.LIVINGROOM, "Im");
		PLACE_PREPOSITIONS.put(Place.OUTSIDE, "");
		PLACE_PREPOSITIONS.put(Place.TERRACE, "Auf der");
	}

	private PlacePrepositions() {
		super();
	}

	public static String getPreposition(Place place) {
		if (PLACE_PREPOSITIONS.containsKey(place)) {
			return PLACE_PREPOSITIONS.get(place);
		} else {
			return FALLBACK_PREPOSITION;
		}
	}
}
