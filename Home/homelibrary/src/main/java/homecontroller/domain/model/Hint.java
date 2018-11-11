package homecontroller.domain.model;

public enum Hint {

	OPEN_WINDOW("Fenster öffnen"), //
	CLOSE_ROLLER_SHUTTER("Rolllade schließen"), //
	;

	private String text;

	private Hint(String text) {
		this.text = text;
	}

	public String formatWithRoomName(RoomClimate roomClimate) {
		if (text == null || text.trim().length() == 0) {
			return null;
		}
		return roomClimate.getPlaceName() + ": " + text;
	}

	public String getText() {
		return text;
	}

}
