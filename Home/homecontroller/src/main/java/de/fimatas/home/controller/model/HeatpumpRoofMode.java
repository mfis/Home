package de.fimatas.home.controller.model;

import lombok.Getter;

@Getter
public enum HeatpumpRoofMode {
    COOLING("Cool"), DRYING("Dry"), FAN("Fan"), AUTO("Auto"), HEATING("Heat");

    private final String value;

    HeatpumpRoofMode(String value) {
        this.value = value;
    }

    public static HeatpumpRoofMode getByValue(String value) {
        for (HeatpumpRoofMode mode : HeatpumpRoofMode.values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("No Heatpump mode with value " + value);
    }
}
