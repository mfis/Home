package homelibrary.homematic.model;

enum HomematicValueFormat {

	DEC("D"), CHAR("C");

	private String historianPrefix;

	private HomematicValueFormat(String historianPrefix) {
		this.historianPrefix = historianPrefix;
	}

	public String getHistorianPrefix() {
		return historianPrefix;
	}
}