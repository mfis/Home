package domain;

public enum Intensity {

	NO("", ""), //
	LOW("leicht sonnig", "leicht aufgeheizt"), //
	MEDIUM("sonnig", "aufgeheizt"), //
	HIGH("stark sonnig", "stark aufgeheizt"), //
	;

	private String sun;

	private String heating;

	private Intensity(String sun, String heating) {
		this.sun = sun;
		this.heating = heating;
	}

	public static Intensity max(Intensity a, Intensity b) {
		int max = Math.max(a.ordinal(), b.ordinal());
		for (Intensity i : values()) {
			if (max == i.ordinal()) {
				return i;
			}
		}
		throw new IllegalStateException();
	}

	public String getSun() {
		return sun;
	}

	public String getHeating() {
		return heating;
	}
}
