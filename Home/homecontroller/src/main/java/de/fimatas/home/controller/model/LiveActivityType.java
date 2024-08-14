package de.fimatas.home.controller.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum LiveActivityType {

    ELECTRICITY("Strom / PV", LiveActivityField.ELECTRIC_GRID, LiveActivityField.PV_BATTERY, LiveActivityField.EV_CHARGE)
    ;

    private final String name;
    private final LiveActivityField primary;
    private final LiveActivityField secondary;
    private final LiveActivityField tertiary;

    LiveActivityType(String name, LiveActivityField primary, LiveActivityField secondary, LiveActivityField tertiary) {
        this.name = name;
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
    }

    public List<LiveActivityField> fields(){
        return Arrays.asList(primary, secondary, tertiary);
    }
}
