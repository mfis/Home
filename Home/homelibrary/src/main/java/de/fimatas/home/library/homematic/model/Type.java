package de.fimatas.home.library.homematic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum Type {

    // regular types
    THERMOMETER("Thermometer", null, true, false), //
    SUN_SENSOR("Sonnensensor", null, true, false), //
    SHUTTER_LEFT("Rolllade links", null, false, true), //
    SHUTTER_RIGHT("Rolllade rechts", null, false, true), //
    SWITCH_WINDOWLIGHT("Schalter Fensterlicht", "Fensterlicht", false, true), //
    SWITCH_FRONTDOOR_CAMERA("Schalter Kamera", "Kamera", false, true), //
    SWITCH_VENTILATION("Schalter Lüftung", "Lüftung", false, true), //
    DOORBELL("Türklingel", null, true, true), //
    DOORLOCK("Türschloss", null, true, true), //
    SWITCH_WALLBOX("Schalter", null, false, true), //
    ELECTRIC_POWER("Strom", null, true, false), //
    ELECTRIC_POWERFEED("Stromeinspeisung", null, true, false), //
    ELECTRIC_POWERPURCHASE("Strombezug", null, true, false), //
    GAS_POWER("Gas", null, true, false), //
    WINDOW_SENSOR("Fenster", "Fenster", true, false), //
    // with sub-types
    THERMOSTAT("Thermostat", null, true, true, Type.THERMOMETER), //
    // pseudo-types
    SYSVAR_THERMOMETER("ConclusionTemperature", null, false, false, Type.THERMOMETER), //
    // pseudo-types electric power
    SYSVAR_ELECTRIC_POWER_GRID_ACTUAL("ElectricPowerGridActual", null, false, false), //
    SYSVAR_ELECTRIC_POWER_PRODUCTION_ACTUAL("ElectricPowerProductionActual", null, false, false), //
    SYSVAR_ELECTRIC_POWER_CONSUMPTION_ACTUAL("ElectricPowerConsumptionActual", null, false, false), //
    SYSVAR_ELECTRIC_POWER_CONSUMPTION_COUNTER("ElectricPowerConsumptionCounter", null, false, false), //
    SYSVAR_ELECTRIC_POWER_PRODUCTION_COUNTER("ElectricPowerProductionCounter", null, false, false), //
    SYSVAR_ELECTRIC_POWER_ACTUAL_TIMESTAMP("ElectricPowerActualTimestamp", null, false, false), //
    ;

    protected static final List<String> VAR_PREFIXES_SWITCH_AUTO = Arrays.asList("Automatic", "AutomaticInfoText");

    protected static final List<String> VAR_PREFIXES_TIMESTAMP = Arrays.asList("Timestamp");

    protected static final List<String> VAR_PREFIXES_DOORLOCK =
        Arrays.asList("Automatic", "AutomaticEvent", "AutomaticInfoText", "IsOpened", "Busy");

    protected static final List<String> VAR_PREFIXES_PROG_CONTROL = Arrays.asList("Busy");

    private final String typeName;

    private final String shortName;

    private final boolean controllable;

    private final boolean hasBattery;

    private final List<Type> subTypes = new ArrayList<>();

    private Type(String typeName, String shortName, boolean hasBattery, boolean controllable, Type... subTypes) {
        this.typeName = typeName;
        this.shortName = shortName;
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

    public String getShortName() {
        return shortName != null ? shortName : typeName;
    }

}
