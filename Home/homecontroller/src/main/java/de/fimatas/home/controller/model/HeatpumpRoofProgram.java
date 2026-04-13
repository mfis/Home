package de.fimatas.home.controller.model;

import lombok.Getter;

@Getter
public enum HeatpumpRoofProgram {

    COOLING_AUTO(true, HeatpumpRoofMode.COOLING, 20, HeatpumpRoofFanSpeed.AUTO), //
    COOLING_MIN(true, HeatpumpRoofMode.COOLING, 20, HeatpumpRoofFanSpeed.ONE), //
    HEATING_AUTO(true, HeatpumpRoofMode.HEATING, 20, HeatpumpRoofFanSpeed.AUTO), //
    HEATING_MIN(true, HeatpumpRoofMode.HEATING, 20, HeatpumpRoofFanSpeed.ONE), //
    FAN_AUTO(true, HeatpumpRoofMode.FAN, 20, HeatpumpRoofFanSpeed.AUTO), //
    FAN_MIN(true, HeatpumpRoofMode.FAN, 20, HeatpumpRoofFanSpeed.ONE), //
    FAN_DRY(true, HeatpumpRoofMode.FAN, 20, HeatpumpRoofFanSpeed.FIVE), //
    OFF(false, HeatpumpRoofMode.FAN, 20, HeatpumpRoofFanSpeed.ONE) //
    ;

    private final boolean expectedOnOffState;

    private final HeatpumpRoofMode expectedMode;

    private final Integer expectedTemperature;

    private final HeatpumpRoofFanSpeed fanSpeed;

    HeatpumpRoofProgram(boolean expectedOnOffState, HeatpumpRoofMode expectedMode, Integer expectedTemperature, HeatpumpRoofFanSpeed fanSpeed){
        this.expectedOnOffState = expectedOnOffState;
        this.expectedMode = expectedMode;
        this.expectedTemperature = expectedTemperature;
        this.fanSpeed = fanSpeed;
    }

}
