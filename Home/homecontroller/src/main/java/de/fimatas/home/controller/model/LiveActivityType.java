package de.fimatas.home.controller.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum LiveActivityType {

    ELECTRICITY("Strom / PV", LiveActivityField.PV_PRODUCTION, LiveActivityField.PV_BATTERY, LiveActivityField.HOUSE_CONSUMPTION, LiveActivityField.EV_CHARGE)
    ;

    private final String name;
    private final LiveActivityField primary;
    private final LiveActivityField secondary;
    private final LiveActivityField tertiary;
    private final LiveActivityField quaternary;

    LiveActivityType(String name, LiveActivityField primary, LiveActivityField secondary, LiveActivityField tertiary, LiveActivityField quaternary) {
        this.name = name;
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
        this.quaternary = quaternary;
    }

    public List<LiveActivityField> fields(){
        return Arrays.asList(primary, secondary, tertiary, quaternary);
    }
}
