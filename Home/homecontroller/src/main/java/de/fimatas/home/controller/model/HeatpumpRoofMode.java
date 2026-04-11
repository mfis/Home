package de.fimatas.home.controller.model;

import lombok.Getter;

@Getter
public enum HeatpumpRoofMode {
    COOLING(3), DRYING(2), FAN(7), AUTO(8), HEATING(1);

    private final int value;

    HeatpumpRoofMode(int value) {
        this.value = value;
    }

    public static HeatpumpRoofMode getByValue(int value) {
        for (HeatpumpRoofMode mode : HeatpumpRoofMode.values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        throw new IllegalArgumentException("No Heatpump mode with value " + value);
    }
}
