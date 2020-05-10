package de.fimatas.home.library.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum Type {

    // regular types
    THERMOMETER("Thermometer", false), //
    SUN_SENSOR("Sonnensensor", false), //
    SHUTTER_LEFT("Rolllade links", true), //
    SHUTTER_RIGHT("Rolllade rechts", true), //
    SWITCH_WINDOWLIGHT("Schalter Fensterlicht", true), //
    SWITCH_FRONTDOOR_CAMERA("Schalter Kamera", true), //
    DOORBELL("Türklingel", true), //
    ELECTRIC_POWER("Stromverbrauch", false), //
    // with sub-types
    THERMOSTAT("Thermostat", true, Type.THERMOMETER), //
    // pseudo-types
    CONCLUSION_OUTSIDE_TEMPERATURE("ConclusionTemperatureDraussen", false, Type.THERMOMETER), //
    DOORBELL_TIMESTAMP_HISTORY("LastBellTimestampHaustuer", false), //
    ;

    private final String typeName;

    private final boolean controllable;

    private final List<Type> subTypes = new ArrayList<>();

    private Type(String typeName, boolean controllable, Type... subTypes) {
        this.typeName = typeName;
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

}
