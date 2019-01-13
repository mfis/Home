package homecontroller.domain.model;

public enum Place {

	LIVINGROOM("Wohnzimmer"), //
	KITCHEN("Küche"), //
	KIDSROOM("Kinderzimmer"), //
	BATHROOM("Bad"), //
	BEDROOM("Schlafzimmer"), //
	HOUSE("Haus"), //
	OUTSIDE("Draußen"), //
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
