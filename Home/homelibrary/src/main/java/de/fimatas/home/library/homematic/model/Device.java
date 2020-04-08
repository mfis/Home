package de.fimatas.home.library.homematic.model;

import java.util.List;

import de.fimatas.home.library.domain.model.AutomationState;
import de.fimatas.home.library.domain.model.Place;
import de.fimatas.home.library.util.HomeUtils;

public enum Device {

	THERMOSTAT_BAD(HomematicProtocol.HM, Type.THERMOSTAT, Place.BATHROOM, true, Datapoint.LIST_THERMOSTAT_HM,
			Type.VAR_PREFIXES_PROG_CONTROL, Boolean.class, Integer.class), //

	THERMOMETER_KINDERZIMMER(HomematicProtocol.HMIP, Type.THERMOMETER, Place.KIDSROOM, true,
			Datapoint.LIST_THERMOMETER_HMIP, null), //

	THERMOMETER_WOHNZIMMER(HomematicProtocol.HMIP, Type.THERMOMETER, Place.LIVINGROOM, true,
			Datapoint.LIST_THERMOMETER_HMIP, null), //

	THERMOMETER_SCHLAFZIMMER(HomematicProtocol.HMIP, Type.THERMOMETER, Place.BEDROOM, true,
			Datapoint.LIST_THERMOMETER_HMIP, null), //

	THERMOMETER_WASCHKUECHE(HomematicProtocol.HMIP, Type.THERMOMETER, Place.LAUNDRY, true,
			Datapoint.LIST_THERMOMETER_HMIP, null), //

	DIFF_TEMPERATUR_TERRASSE_AUSSEN(HomematicProtocol.HM, Type.THERMOMETER, Place.TERRACE, false,
			Datapoint.LIST_DIFFTHERMOMETER_HM, null), //

	DIFF_TEMPERATUR_TERRASSE_DIFF(HomematicProtocol.HM, Type.SUN_SENSOR, Place.TERRACE, false,
			Datapoint.LIST_DIFFTHERMOMETER_HM, null), //

	DIFF_TEMPERATUR_EINFAHRT_AUSSEN(HomematicProtocol.HM, Type.THERMOMETER, Place.ENTRANCE, false,
			Datapoint.LIST_DIFFTHERMOMETER_HM, null), //

	DIFF_TEMPERATUR_EINFAHRT_DIFF(HomematicProtocol.HM, Type.SUN_SENSOR, Place.ENTRANCE, false,
			Datapoint.LIST_DIFFTHERMOMETER_HM, null), //

	SCHALTER_KUECHE_LICHT(HomematicProtocol.HM, Type.SWITCH_WINDOWLIGHT, Place.KITCHEN, true, Datapoint.LIST_SWITCH_HM,
			Type.VAR_PREFIXES_SWITCH_AUTO, Boolean.class, AutomationState.class), //

	STROMZAEHLER(HomematicProtocol.HM, Type.ELECTRIC_POWER, Place.HOUSE, true, Datapoint.LIST_POWERMETER_HM, null), //

	AUSSENTEMPERATUR(HomematicProtocol.SYSVAR, Type.SYSVAR_THERMOMETER, Place.OUTSIDE, true, Datapoint.LIST_SYSVAR,
			null), //

	HAUSTUER_KLINGEL(HomematicProtocol.HM, Type.DOORBELL, Place.FRONTDOOR, false, Datapoint.LIST_DOORBELL, null), //

	HAUSTUER_KLINGEL_HISTORIE(HomematicProtocol.SYSVAR, Type.SYSVAR_LAST_BELL_TIMESTAMP, Place.FRONTDOOR, false,
			Datapoint.LIST_SYSVAR, null), //

	HAUSTUER_KAMERA(HomematicProtocol.HM, Type.SWITCH_FRONTDOOR_CAMERA, Place.FRONTDOOR, false, Datapoint.LIST_CAMERA,
			null), //
	
	HAUSTUER_SCHLOSS(HomematicProtocol.HM, Type.DOORLOCK, Place.FRONTDOOR, false, Datapoint.LIST_DOORLOCK, Type.VAR_PREFIXES_SWITCH_AUTO,
			null, null), //

	// ROLLLADE_SCHLAFZIMMER_LINKS(HomematicProtocol.HM, "D_U_M_M_Y", 1,
	// Type.SHUTTER_LEFT, Place.BEDROOM, false,
	// Integer.class, ShutterPosition.class), //

	;

	private HomematicProtocol homematicProtocol;

	private Type type;

	private Place place;

	private boolean textQueryEnabled;

	private List<Datapoint> datapoints;

	private List<String> sysVars;

	private Class<?>[] valueTypes;

	private Device(HomematicProtocol homematicProtocol, Type type, Place place, // NOSONAR
			boolean textQueryEnabled, List<Datapoint> datapoints, List<String> sysVars, Class<?>... valueTypes) {
		
		this.homematicProtocol = homematicProtocol;
		this.type = type;
		this.place = place;
		this.textQueryEnabled = textQueryEnabled;
		this.datapoints = datapoints;
		this.sysVars = sysVars;
		this.valueTypes = valueTypes;
	}

	public boolean isControllable() {
		return valueTypes != null && valueTypes.length > 0;
	}

	public String programNamePrefix() {
		return HomeUtils.escape(getDescription());
	}

	public Datapoint lowBatDatapoint() {
		if (isHomematic()) {
			return Datapoint.LOWBAT;
		} else if (isHomematicIP()) {
			return Datapoint.LOW_BAT;
		} else {
			return null;
		}
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

	public List<Datapoint> getDatapoints() {
		return datapoints;
	}

	public List<String> getSysVars() {
		return sysVars;
	}

}
