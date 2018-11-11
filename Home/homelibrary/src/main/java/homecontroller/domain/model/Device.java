package homecontroller.domain.model;

import org.springframework.util.StringUtils;

public enum Device {

	THERMOSTAT_BAD(Protocol.HM, "OEQ0854602", 4, "Thermostat", "Bad"), //
	THERMOMETER_KINDERZIMMER(Protocol.HMIP, "000E97099314A3", 1, "Thermometer", "Kinderzimmer"), //
	THERMOMETER_WOHNZIMMER(Protocol.HMIP, "000E97099312D5", 1, "Thermometer", "Wohnzimmer"), //
	THERMOMETER_SCHLAFZIMMER(Protocol.HMIP, "000E97099314C4", 1, "Thermometer", "Schlafzimmer"), //
	DIFFERENZTEMPERATUR_TERRASSE_AUSSEN(Protocol.HM, "OEQ0801741", 2, "Thermometer", "Terrasse"), //
	DIFFERENZTEMPERATUR_TERRASSE_DIFF(Protocol.HM, "OEQ0801741", 3, "Sonnensensor", "Terrasse"), //
	DIFFERENZTEMPERATUR_EINFAHRT_AUSSEN(Protocol.HM, "OEQ0801807", 2, "Thermometer", "Einfahrt"), //
	DIFFERENZTEMPERATUR_EINFAHRT_DIFF(Protocol.HM, "OEQ0801807", 3, "Sonnensensor", "Einfahrt"), //
	SCHALTER_KUECHE_LICHT(Protocol.HM, "OEQ0712456", 1, "Schalter Fensterlicht", "Küche"), //
	STROMZAEHLER(Protocol.HM, "NEQ0861520", 1, "Stromverbrauch", "Haus"), //
	;

	private Protocol protocol;

	private String id;

	private int channel;

	private String type;

	private String placeName;

	private Device(Protocol protocol, String id, int channel, String type, String placeName) {
		this.protocol = protocol;
		this.id = id;
		this.channel = channel;
		this.type = type;
		this.placeName = placeName;
	}

	public String accessKeyXmlApi(Datapoint datapoint) {
		return protocol.toXmlApiString() + "." + id + ":" + Integer.toString(channel) + "."
				+ datapoint.name();
	}

	public String accessMainDeviceKeyXmlApi(Datapoint datapoint) {
		return protocol.toXmlApiString() + "." + id + ":" + Integer.toString(0) + "." + datapoint.name();
	}

	public String accessKeyHistorian(Datapoint datapoint) {
		return datapoint.getHistorianPrefix() + "_" + protocol.toHistorianString() + "_" + id + "_"
				+ Integer.toString(channel) + "_" + datapoint.name();
	}

	public String programNamePrefix() {
		String prefix = getDescription();
		prefix = StringUtils.replace(prefix, " ", "");
		prefix = StringUtils.replace(prefix, "ä", "ae"); // FIXME
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

		HM("BidCos"), HMIP("HmIP");

		private String protocol;

		private final static String RF = "RF";

		private Protocol(String protocol) {
			this.protocol = protocol;
		}

		public String toXmlApiString() {
			return protocol + "-" + RF;
		}

		public String toHistorianString() {
			return protocol.toUpperCase() + "_" + RF;
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
