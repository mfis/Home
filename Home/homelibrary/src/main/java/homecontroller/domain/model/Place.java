package homecontroller.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum Place {

	LIVINGROOM("Wohnzimmer"), //
	KITCHEN("Küche"), //
	KIDSROOM("Kinderzimmer"), //
	BATHROOM("Bad"), //
	BEDROOM("Schlafzimmer"), //
	HOUSE("Haus"), //
	ENTRANCE("Einfahrt"), //
	TERRACE("Terrasse"), //
	FRONTDOOR("Haustür"), //
	// with sub-places
	OUTSIDE("Draußen", Place.ENTRANCE, Place.TERRACE), //
	;

	private String placeName;

	private final List<Place> subPlaces = new ArrayList<>();

	private Place(String placeName, Place... subPlaces) {
		this.placeName = placeName;
		if (subPlaces != null) {
			this.subPlaces.addAll(Arrays.asList(subPlaces));
		}
	}

	public String getPlaceName() {
		return placeName;
	}

	public List<Place> getSubPlaces() {
		return subPlaces;
	}

	public List<Place> allPlaces() {
		List<Place> list = new LinkedList<>();
		list.add(this);
		list.addAll(subPlaces);
		return list;
	}

}
