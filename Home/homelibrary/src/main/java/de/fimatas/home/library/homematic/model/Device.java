package de.fimatas.home.library.homematic.model;

import java.util.List;
import de.fimatas.home.library.domain.model.AutomationState;
import de.fimatas.home.library.domain.model.Place;
import de.fimatas.home.library.util.HomeUtils;

public enum Device {

    THERMOSTAT_BAD(HomematicProtocol.HM, Type.THERMOSTAT, Place.BATHROOM, true, Datapoint.LIST_THERMOSTAT_HM,
        Type.VAR_PREFIXES_PROG_CONTROL, Boolean.class, Integer.class), //

    THERMOMETER_KINDERZIMMER_1(HomematicProtocol.HMIP, Type.THERMOMETER, Place.KIDSROOM_1, true, Datapoint.LIST_THERMOMETER_HMIP,
        null), //

    THERMOMETER_KINDERZIMMER_2(HomematicProtocol.HMIP, Type.THERMOMETER, Place.KIDSROOM_2, true, Datapoint.LIST_THERMOMETER_HMIP,
            null), //

    THERMOMETER_WOHNZIMMER(HomematicProtocol.HMIP, Type.THERMOMETER, Place.LIVINGROOM, true, Datapoint.LIST_THERMOMETER_HMIP,
        null), //

    THERMOMETER_SCHLAFZIMMER(HomematicProtocol.HMIP, Type.THERMOMETER, Place.BEDROOM, true, Datapoint.LIST_THERMOMETER_HMIP,
        null), //

    THERMOMETER_WASCHKUECHE(HomematicProtocol.HMIP, Type.THERMOMETER, Place.LAUNDRY, true, Datapoint.LIST_THERMOMETER_HMIP,
        null), //

    THERMOMETER_GARTEN(HomematicProtocol.HMIP, Type.THERMOMETER, Place.GARDEN, true, Datapoint.LIST_THERMOMETER_HMIP, null), //

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

    STROMZAEHLER_GESAMT(HomematicProtocol.HM, Type.ELECTRIC_POWER, Place.HOUSE, true, Datapoint.LIST_POWERMETER_HM,
        Type.VAR_PREFIXES_TIMESTAMP),

    STROMZAEHLER_WALLBOX(HomematicProtocol.HM, Type.ELECTRIC_POWER, Place.WALLBOX, true, Datapoint.LIST_POWERMETER_HM,
        Type.VAR_PREFIXES_TIMESTAMP),

    SCHALTER_WALLBOX(HomematicProtocol.HM, Type.SWITCH_WALLBOX, Place.WALLBOX, true, Datapoint.LIST_SWITCH_HM,
        Type.VAR_PREFIXES_SWITCH_AUTO, Boolean.class, AutomationState.class),

    AUSSENTEMPERATUR(HomematicProtocol.SYSVAR, Type.SYSVAR_THERMOMETER, Place.OUTSIDE, true, Datapoint.LIST_SYSVAR, null), //

    HAUSTUER_KLINGEL(HomematicProtocol.HM, Type.DOORBELL, Place.FRONTDOOR, false, Datapoint.LIST_DOORBELL,
        Type.VAR_PREFIXES_TIMESTAMP), //

    // HAUSTUER_KAMERA(HomematicProtocol.HM, Type.SWITCH_FRONTDOOR_CAMERA, Place.FRONTDOOR, false, Datapoint.LIST_CAMERA, null),
    // //

    HAUSTUER_SCHLOSS(HomematicProtocol.HM, Type.DOORLOCK, Place.FRONTDOOR, false, Datapoint.LIST_DOORLOCK,
        Type.VAR_PREFIXES_DOORLOCK, null, null), //

    SCHALTER_WERKSTATT_LUEFTUNG(HomematicProtocol.HMIP, Type.SWITCH_VENTILATION, Place.WORKSHOP, true, Datapoint.LIST_SWITCH_HM,
        Type.VAR_PREFIXES_SWITCH_AUTO, Boolean.class, AutomationState.class), //

    FENSTERSENSOR_GAESTEZIMMER(HomematicProtocol.HMIP, Type.WINDOW_SENSOR, Place.GUESTROOM, false, Datapoint.LIST_WINDOW_SENSOR,
        Type.VAR_PREFIXES_TIMESTAMP),

    FENSTERSENSOR_WERKSTATT(HomematicProtocol.HMIP, Type.WINDOW_SENSOR, Place.WORKSHOP, false, Datapoint.LIST_WINDOW_SENSOR,
        Type.VAR_PREFIXES_TIMESTAMP),

    FENSTERSENSOR_WASCHKUECHE(HomematicProtocol.HMIP, Type.WINDOW_SENSOR, Place.LAUNDRY, false, Datapoint.LIST_WINDOW_SENSOR,
        Type.VAR_PREFIXES_TIMESTAMP),

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

    public String historyKeyPrefix() {
        return HomeUtils.escape(getHistoryKey());
    }

    public Datapoint lowBatDatapoint() {
        if (!getType().isHasBattery()) {
            return null;
        } else if (isHomematic()) {
            return Datapoint.LOWBAT;
        } else if (isHomematicIP()) {
            return Datapoint.LOW_BAT;
        } else {
            throw new IllegalStateException("unknown protocol type for device with battery");
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

    public String getHistoryKey() {
        return type.getTypeName() + " " + place.name();
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
