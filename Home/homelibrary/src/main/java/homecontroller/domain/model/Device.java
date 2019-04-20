package homecontroller.domain.model;

import org.springframework.util.StringUtils;

public enum Device {

	// @formatter:off
	THERMOSTAT_BAD(Protocol.HM, "OEQ0854602", 4, Type.THERMOSTAT, Place.BATHROOM, true, Boolean.class,
			Integer.class), //
	THERMOMETER_KINDERZIMMER(Protocol.HMIP, "000E97099314A3", 1, Type.THERMOMETER, Place.KIDSROOM, true), //
	THERMOMETER_WOHNZIMMER(Protocol.HMIP, "000E97099312D5", 1, Type.THERMOMETER, Place.LIVINGROOM, true), //
	THERMOMETER_SCHLAFZIMMER(Protocol.HMIP, "000E97099314C4", 1, Type.THERMOMETER, Place.BEDROOM, true), //
	ROLLLADE_SCHLAFZIMMER_LINKS(Protocol.HM, "D_U_M_M_Y", 1, Type.SHUTTER_LEFT, Place.BEDROOM, false,
			Integer.class, ShutterPosition.class), //
	DIFF_TEMPERATUR_TERRASSE_AUSSEN(Protocol.HM, "OEQ0801741", 2, Type.THERMOMETER, Place.TERRACE, false), //
	DIFF_TEMPERATUR_TERRASSE_DIFF(Protocol.HM, "OEQ0801741", 3, Type.SUN_SENSOR, Place.TERRACE, false), //
	DIFF_TEMPERATUR_EINFAHRT_AUSSEN(Protocol.HM, "OEQ0801807", 2, Type.THERMOMETER, Place.ENTRANCE, false), //
	DIFF_TEMPERATUR_EINFAHRT_DIFF(Protocol.HM, "OEQ0801807", 3, Type.SUN_SENSOR, Place.ENTRANCE, false), //
	SCHALTER_KUECHE_LICHT(Protocol.HM, "OEQ0712456", 1, Type.SWITCH_WINDOWLIGHT, Place.KITCHEN, true,
			Boolean.class, AutomationState.class), //
	STROMZAEHLER(Protocol.HM, "NEQ0861520", 1, Type.ELECTRIC_POWER, Place.HOUSE, true), //
	AUSSENTEMPERATUR(Protocol.SYSVAR, "2867", null, Type.CONCLUSION_OUTSIDE_TEMPERATURE, Place.OUTSIDE, true), //
	HAUSTUER_KAMERA(Protocol.HM, "??????????", 1, Type.SWITCH_FRONTDOOR_CAMERA, Place.FRONTDOOR, false), //
	// @formatter:on
	;

	private Protocol protocol;

	private String id;

	private Integer channel;

	private Type type;

	private Place place;

	private boolean textQueryEnabled;

	private Class<?>[] valueTypes;

	private Device(Protocol protocol, String id, Integer channel, Type type, Place place,
			boolean textQueryEnabled, Class<?>... valueTypes) {
		this.protocol = protocol;
		this.id = id;
		this.channel = channel;
		this.type = type;
		this.place = place;
		this.textQueryEnabled = textQueryEnabled;
		this.valueTypes = valueTypes;
	}

	public boolean isControllable() {
		return valueTypes != null && valueTypes.length > 0;
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

	public boolean isTextQueryEnabled() {
		return textQueryEnabled;
	}

	public Class<?>[] getValueTypes() {
		return valueTypes;
	}

}
