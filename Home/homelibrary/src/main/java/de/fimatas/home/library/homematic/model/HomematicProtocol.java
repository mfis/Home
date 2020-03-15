package de.fimatas.home.library.homematic.model;

public enum HomematicProtocol {

	HM("BidCos"), HMIP("HmIP"), SYSVAR("SysVar");

	private String key;

	public static final String RF = "RF";

	private HomematicProtocol(String protocol) {
		this.key = protocol;
	}

	public String toHistorianString() {
		return key.toUpperCase() + (key.equals(SYSVAR.key) ? "" : "_" + RF);
	}

	public String getKey() {
		return key;
	}
}