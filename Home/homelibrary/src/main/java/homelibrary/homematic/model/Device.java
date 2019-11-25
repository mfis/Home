package homelibrary.homematic.model;

import java.util.List;

import homecontroller.domain.model.AutomationState;
import homecontroller.domain.model.Place;
import homecontroller.domain.model.Type;

public enum Device {

	THERMOSTAT_BAD(HomematicProtocol.HM, "OEQ0854602", 4, Type.THERMOSTAT, Place.BATHROOM, true,
			Datapoint.LIST_THERMOSTAT_HM, Boolean.class, Integer.class), //

	THERMOMETER_KINDERZIMMER(HomematicProtocol.HMIP, "000E97099314A3", 1, Type.THERMOMETER, Place.KIDSROOM,
			true, Datapoint.LIST_THERMOMETER_HMIP), //

	THERMOMETER_WOHNZIMMER(HomematicProtocol.HMIP, "000E97099312D5", 1, Type.THERMOMETER, Place.LIVINGROOM,
			true, Datapoint.LIST_THERMOMETER_HMIP), //

	THERMOMETER_SCHLAFZIMMER(HomematicProtocol.HMIP, "000E97099314C4", 1, Type.THERMOMETER, Place.BEDROOM,
			true, Datapoint.LIST_THERMOMETER_HMIP), //

	THERMOMETER_WASCHKUECHE(HomematicProtocol.HMIP, "000E9A498BA811", 1, Type.THERMOMETER, Place.LAUNDRY,
			true, Datapoint.LIST_THERMOMETER_HMIP), //

	// ROLLLADE_SCHLAFZIMMER_LINKS(HomematicProtocol.HM, "D_U_M_M_Y", 1,
	// Type.SHUTTER_LEFT, Place.BEDROOM, false,
	// Integer.class, ShutterPosition.class), //

	DIFF_TEMPERATUR_TERRASSE_AUSSEN(HomematicProtocol.HM, "OEQ0801741", 2, Type.THERMOMETER, Place.TERRACE,
			false, Datapoint.LIST_DIFFTHERMOMETER_HM), //

	DIFF_TEMPERATUR_TERRASSE_DIFF(HomematicProtocol.HM, "OEQ0801741", 3, Type.SUN_SENSOR, Place.TERRACE,
			false, Datapoint.LIST_DIFFTHERMOMETER_HM), //

	DIFF_TEMPERATUR_EINFAHRT_AUSSEN(HomematicProtocol.HM, "OEQ0801807", 2, Type.THERMOMETER, Place.ENTRANCE,
			false, Datapoint.LIST_DIFFTHERMOMETER_HM), //

	DIFF_TEMPERATUR_EINFAHRT_DIFF(HomematicProtocol.HM, "OEQ0801807", 3, Type.SUN_SENSOR, Place.ENTRANCE,
			false, Datapoint.LIST_DIFFTHERMOMETER_HM), //

	SCHALTER_KUECHE_LICHT(HomematicProtocol.HM, "OEQ0712456", 1, Type.SWITCH_WINDOWLIGHT, Place.KITCHEN, true,
			Datapoint.LIST_SWITCH_HM, Boolean.class, AutomationState.class), //

	STROMZAEHLER(HomematicProtocol.HM, "NEQ0861520", 1, Type.ELECTRIC_POWER, Place.HOUSE, true,
			Datapoint.LIST_POWERMETER_HM), //

	AUSSENTEMPERATUR(HomematicProtocol.SYSVAR, "ConclusionOutsideTemperature", 2867, Type.SYSVAR_THERMOMETER,
			Place.OUTSIDE, true, Datapoint.LIST_SYSVAR), //

	HAUSTUER_KLINGEL(HomematicProtocol.HM, "PEQ0652576", 1, Type.DOORBELL, Place.FRONTDOOR, false,
			Datapoint.LIST_DOORBELL), //

	HAUSTUER_KLINGEL_HISTORIE(HomematicProtocol.SYSVAR, "doorbellTimestampHistory", 3218, Type.SYSVAR,
			Place.FRONTDOOR, false, Datapoint.LIST_SYSVAR), //

	HAUSTUER_KAMERA(HomematicProtocol.HM, "PEQ0508418", 1, Type.SWITCH_FRONTDOOR_CAMERA, Place.FRONTDOOR,
			false, Datapoint.LIST_CAMERA), //
	;

	private HomematicProtocol homematicProtocol;

	private String id;

	private Integer channel;

	private Type type;

	private Place place;

	private boolean textQueryEnabled;

	private List<Datapoint> datapoints;

	private Class<?>[] valueTypes;

	private Device(HomematicProtocol homematicProtocol, String id, Integer channel, Type type, Place place,
			boolean textQueryEnabled, List<Datapoint> datapoints, Class<?>... valueTypes) {
		this.homematicProtocol = homematicProtocol;
		this.id = id;
		this.channel = channel;
		this.type = type;
		this.place = place;
		this.textQueryEnabled = textQueryEnabled;
		this.datapoints = datapoints;
		this.valueTypes = valueTypes;
	}

	public boolean isControllable() {
		return valueTypes != null && valueTypes.length > 0;
	}

	public String accessKeyHistorian(Datapoint datapoint) {
		if (isSysVar()) {
			return datapoint.getHistorianPrefix() + "_" + homematicProtocol.toHistorianString() + "_"
					+ Integer.toString(channel) + "_" + datapoint.name();
		} else {
			return datapoint.getHistorianPrefix() + "_" + homematicProtocol.toHistorianString() + "_" + id
					+ (channel != null ? ("_" + Integer.toString(channel)) : "") + "_" + datapoint.name();
		}
	}

	public String programNamePrefix() { // TODO: solve circular dependency!
		return HomematicCommand.escape(getDescription());
	}

	public boolean isHomematic() {
		return homematicProtocol == HomematicProtocol.HM;
	}

	public boolean isHomematicIP() {
		return homematicProtocol == HomematicProtocol.HMIP;
	}

	public boolean isSysVar() {
		return homematicProtocol == HomematicProtocol.SYSVAR;
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

	public HomematicProtocol getHomematicProtocol() {
		return homematicProtocol;
	}

	public String getId() {
		return id;
	}

	public Integer getChannel() {
		return channel;
	}

	public List<Datapoint> getDatapoints() {
		return datapoints;
	}

}
