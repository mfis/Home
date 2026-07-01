package de.fimatas.home.library.domain.model;

import lombok.Getter;

@Getter
public enum HeatpumpRoofFanSpeed {

    ONE("Min"), TWO("Low"), THREE("Med"), FOUR("High"), FIVE("Max"), AUTO("Auto");

    private final String value;

    HeatpumpRoofFanSpeed(String value) {
        this.value = value;
    }

    public static HeatpumpRoofFanSpeed getByValue(String value) {
        for (HeatpumpRoofFanSpeed speed : HeatpumpRoofFanSpeed.values()) {
            if (speed.value.equals(value)) {
                return speed;
            }
        }
        throw new IllegalArgumentException("No Heatpump speed with value " + value);
    }
}
