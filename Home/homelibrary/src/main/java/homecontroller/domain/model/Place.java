package homecontroller.domain.model;

public enum Place {

	LIVINGROOM("Wohnzimmer"), //
	KITCHEN("Küche"), //
	KIDSROOM("Kinderzimmer"), //
	BATHROOM("Bad"), //
	BEDROOM("Schlafzimmer"), //
	OUTSIDE("Haus"), //
	HOUSE("Haus"), //
	ENTRANCE("Einfahrt"), //
	TERRACE("Terrasse"), //
	;

	private String placeName;

	private Place(String placeName) {
		this.placeName = placeName;
	}

	public String getPlaceName() {
		return placeName;
	}

}
