package de.fimatas.home.controller.model;

import lombok.Getter;

@Getter
public enum HeatpumpRoofFanSpeed {

    ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), AUTO(0);

    private final int value;

    HeatpumpRoofFanSpeed(int value) {
        this.value = value;
    }

    public static HeatpumpRoofFanSpeed getByValue(int value) {
        for (HeatpumpRoofFanSpeed speed : HeatpumpRoofFanSpeed.values()) {
            if (speed.value == value) {
                return speed;
            }
        }
        throw new IllegalArgumentException("No Heatpump speed with value " + value);
    }
}
