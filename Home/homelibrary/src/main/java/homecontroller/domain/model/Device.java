package homecontroller.domain.model;

import org.springframework.util.StringUtils;

public enum Device {

	THERMOSTAT_BAD(Protocol.HM, "OEQ0854602", 4, "Thermostat", "Bad"), //
	THERMOMETER_KINDERZIMMER(Protocol.HMIP, "000E97099314A3", 1, Const.THERMOMETER, "Kinderzimmer"), //
	THERMOMETER_WOHNZIMMER(Protocol.HMIP, "000E97099312D5", 1, Const.THERMOMETER, "Wohnzimmer"), //
	THERMOMETER_SCHLAFZIMMER(Protocol.HMIP, "000E97099314C4", 1, Const.THERMOMETER, Const.SCHLAFZIMMER), //
	ROLLLADE_SCHLAFZIMMER_LINKS(Protocol.HM, "D_U_M_M_Y", 1, "Rolllade links", Const.SCHLAFZIMMER), //
	DIFFERENZTEMPERATUR_TERRASSE_AUSSEN(Protocol.HM, "OEQ0801741", 2, Const.THERMOMETER, Const.TERRASSE), //
	DIFFERENZTEMPERATUR_TERRASSE_DIFF(Protocol.HM, "OEQ0801741", 3, Const.SONNENSENSOR, Const.TERRASSE), //
	DIFFERENZTEMPERATUR_EINFAHRT_AUSSEN(Protocol.HM, "OEQ0801807", 2, Const.THERMOMETER, Const.EINFAHRT), //
	DIFFERENZTEMPERATUR_EINFAHRT_DIFF(Protocol.HM, "OEQ0801807", 3, Const.SONNENSENSOR, Const.EINFAHRT), //
	SCHALTER_KUECHE_LICHT(Protocol.HM, "OEQ0712456", 1, "Schalter Fensterlicht", "Küche"), //
	STROMZAEHLER(Protocol.HM, "NEQ0861520", 1, "Stromverbrauch", "Haus"), //
	AUSSENTEMPERATUR(Protocol.SYSVAR, "2867", null, "ConclusionOutsideTemperature", "Aussen"), //
	;

	private Protocol protocol;

	private String id;

	private Integer channel;

	private String type;

	private String placeName;

	private Device(Protocol protocol, String id, Integer channel, String type, String placeName) {
		this.protocol = protocol;
		this.id = id;
		this.channel = channel;
		this.type = type;
		this.placeName = placeName;
	}

	private class Const {
		public static final String THERMOMETER = "Thermometer";
		public static final String SONNENSENSOR = "Sonnensensor";
		public static final String SCHLAFZIMMER = "Schlafzimmer";
		public static final String EINFAHRT = "Einfahrt";
		public static final String TERRASSE = "Terrasse";
	}

	public String accessKeyXmlApi(Datapoint datapoint) {
		return protocol.toXmlApiString() + "." + id + ":" + Integer.toString(channel) + "."
				+ datapoint.name();
	}

	public String accessMainDeviceKeyXmlApi(Datapoint datapoint) {
		return protocol.toXmlApiString() + "." + id + ":" + Integer.toString(0) + "." + datapoint.name();
	}

	public String accessKeyHistorian(Datapoint datapoint) {
		return datapoint.getHistorianPrefix() + "_" + protocol.toHistorianString() + "_" + id
				+ (channel != null ? ("_" + Integer.toString(channel)) : "") + "_" + datapoint.name();
	}

	public String programNamePrefix() {
		String prefix = getDescription();
		prefix = StringUtils.replace(prefix, " ", "");
		prefix = StringUtils.replace(prefix, "ä", "ae");
		prefix = StringUtils.replace(prefix, "ö", "oe");
		prefix = StringUtils.replace(prefix, "ü", "ue");
		prefix = StringUtils.replace(prefix, "Ä", "Ae");
		prefix = StringUtils.replace(prefix, "Ö", "Oe");
		prefix = StringUtils.replace(prefix, "Ü", "Ue");
		prefix = StringUtils.replace(prefix, "ß", "ss");
		return prefix;
	}

	public boolean isHomematic() {
		return protocol == Protocol.HM;
	}

	public boolean isHomematicIP() {
		return protocol == Protocol.HMIP;
	}

	private enum Protocol {

		HM("BidCos"), HMIP("HmIP"), SYSVAR("SysVar");

		private String key;

		private static final String RF = "RF";

		private Protocol(String protocol) {
			this.key = protocol;
		}

		public String toXmlApiString() {
			return key + (key.equals(SYSVAR.key) ? "" : "-" + RF);
		}

		public String toHistorianString() {
			return key.toUpperCase() + (key.equals(SYSVAR.key) ? "" : "_" + RF);
		}
	}

	public String getDescription() {
		return type + " " + placeName;
	}

	public String getType() {
		return type;
	}

	public String getPlaceName() {
		return placeName;
	}

}
