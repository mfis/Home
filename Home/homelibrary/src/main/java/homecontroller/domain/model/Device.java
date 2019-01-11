package homecontroller.domain.model;

import org.springframework.util.StringUtils;

public enum Device {

	THERMOSTAT_BAD(Protocol.HM, "OEQ0854602", 4, Type.THERMOSTAT, Place.BATHROOM), //
	THERMOMETER_KINDERZIMMER(Protocol.HMIP, "000E97099314A3", 1, Type.THERMOMETER, Place.KIDSROOM), //
	THERMOMETER_WOHNZIMMER(Protocol.HMIP, "000E97099312D5", 1, Type.THERMOMETER, Place.LIVINGROOM), //
	THERMOMETER_SCHLAFZIMMER(Protocol.HMIP, "000E97099314C4", 1, Type.THERMOMETER, Place.BEDROOM), //
	ROLLLADE_SCHLAFZIMMER_LINKS(Protocol.HM, "D_U_M_M_Y", 1, Type.SHUTTER_LEFT, Place.BEDROOM), //
	DIFFERENZTEMPERATUR_TERRASSE_AUSSEN(Protocol.HM, "OEQ0801741", 2, Type.THERMOMETER, Place.TERRACE), //
	DIFFERENZTEMPERATUR_TERRASSE_DIFF(Protocol.HM, "OEQ0801741", 3, Type.SUN_SENSOR, Place.TERRACE), //
	DIFFERENZTEMPERATUR_EINFAHRT_AUSSEN(Protocol.HM, "OEQ0801807", 2, Type.THERMOMETER, Place.ENTRANCE), //
	DIFFERENZTEMPERATUR_EINFAHRT_DIFF(Protocol.HM, "OEQ0801807", 3, Type.SUN_SENSOR, Place.ENTRANCE), //
	SCHALTER_KUECHE_LICHT(Protocol.HM, "OEQ0712456", 1, Type.SWITCH_WINDOWLIGHT, Place.KITCHEN), //
	STROMZAEHLER(Protocol.HM, "NEQ0861520", 1, Type.ELECTRIC_POWER, Place.HOUSE), //
	AUSSENTEMPERATUR(Protocol.SYSVAR, "2867", null, Type.CONCLUSION_OUTSIDE_TEMPERATURE, Place.OUTSIDE), //
	;

	private Protocol protocol;

	private String id;

	private Integer channel;

	private Type type;

	private Place place;

	private Device(Protocol protocol, String id, Integer channel, Type type, Place place) {
		this.protocol = protocol;
		this.id = id;
		this.channel = channel;
		this.type = type;
		this.place = place;
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
		return type.getTypeName() + " " + place.getPlaceName();
	}

	public Type getType() {
		return type;
	}

	public Place getPlace() {
		return place;
	}

}
