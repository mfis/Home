package de.fimatas.home.library.homematic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum Type {

    // regular types
    THERMOMETER("Thermometer", true, false), //
    SUN_SENSOR("Sonnensensor", true, false), //
    SHUTTER_LEFT("Rolllade links", false, true), //
    SHUTTER_RIGHT("Rolllade rechts", false, true), //
    SWITCH_WINDOWLIGHT("Schalter Fensterlicht", false, true), //
    SWITCH_FRONTDOOR_CAMERA("Schalter Kamera", false, true), //
    SWITCH_VENTILATION("Schalter Lüftung", false, true), //
    DOORBELL("Türklingel", true, true), //
    DOORLOCK("Türschloss", true, true), //
    SWITCH_WALLBOX("Schalter", false, true), //
    ELECTRIC_POWER("Stromverbrauch", true, false), //
    // with sub-types
    THERMOSTAT("Thermostat", true, true, Type.THERMOMETER), //
    // pseudo-types
    SYSVAR_LAST_BELL_TIMESTAMP("LastBellTimestamp", false, false), //
    SYSVAR_THERMOMETER("ConclusionTemperature", false, false, Type.THERMOMETER), //
    ;

    protected static final List<String> VAR_PREFIXES_SWITCH_AUTO = Arrays.asList("Automatic", "AutomaticInfoText");

    protected static final List<String> VAR_PREFIXES_DOORLOCK =
        Arrays.asList("Automatic", "AutomaticEvent", "AutomaticInfoText", "IsOpened", "Busy");

    protected static final List<String> VAR_PREFIXES_PROG_CONTROL = Arrays.asList("Busy");

    private final String typeName;

    private final boolean controllable;

    private final boolean hasBattery;

    private final List<Type> subTypes = new ArrayList<>();

    private Type(String typeName, boolean hasBattery, boolean controllable, Type... subTypes) {
        this.typeName = typeName;
        this.hasBattery = hasBattery;
        this.controllable = controllable;
        if (subTypes != null) {
            this.subTypes.addAll(Arrays.asList(subTypes));
        }
    }

    public boolean isControllable() {
        return controllable;
    }

    public String getTypeName() {
        return typeName;
    }

    public List<Type> getSubTypes() {
        return subTypes;
    }

    public List<Type> allTypes() {
        List<Type> list = new LinkedList<>();
        list.add(this);
        list.addAll(subTypes);
        return list;
    }

    public boolean isHasBattery() {
        return hasBattery;
    }

}
