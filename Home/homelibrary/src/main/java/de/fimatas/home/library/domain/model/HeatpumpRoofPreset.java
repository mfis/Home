package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Getter;

@Getter
public enum HeatpumpRoofPreset {

    COOL1_AUTO("Kühlen 20°", "Automatisch", "Kühlen 20°", ConditionColor.BLUE, true, HeatpumpRoofMode.COOLING, 20, HeatpumpRoofFanSpeed.AUTO), //
    COOL1_TIMER1("Kühlen 20°", "Automatisch", "Kühlen 20° 19:30", ConditionColor.BLUE, true, HeatpumpRoofMode.COOLING, 20, HeatpumpRoofFanSpeed.AUTO), //
    COOL1_TIMER2("Kühlen 20°", "Automatisch", "Kühlen 20° 22:00", ConditionColor.BLUE, true, HeatpumpRoofMode.COOLING, 20, HeatpumpRoofFanSpeed.AUTO), //

    COOL2_AUTO("Kühlen 22°", "Automatisch", "Kühlen 22°", ConditionColor.BLUE, true, HeatpumpRoofMode.COOLING, 22, HeatpumpRoofFanSpeed.AUTO), //
    COOL2_TIMER1("Kühlen 22°", "Automatisch", "Kühlen 22° 19:30", ConditionColor.BLUE, true, HeatpumpRoofMode.COOLING, 22, HeatpumpRoofFanSpeed.AUTO), //
    COOL2_TIMER2("Kühlen 22°", "Automatisch", "Kühlen 22° 22:00", ConditionColor.BLUE, true, HeatpumpRoofMode.COOLING, 22, HeatpumpRoofFanSpeed.AUTO), //

    HEAT_AUTO("Heizen 21°", "Automatisch", "Heizen 21°", ConditionColor.ORANGE, true, HeatpumpRoofMode.HEATING, 21, HeatpumpRoofFanSpeed.AUTO), //
    HEAT_TIMER1("Heizen 21°", "Automatisch", "Heizen 21° 2h", ConditionColor.ORANGE, true, HeatpumpRoofMode.HEATING, 21, HeatpumpRoofFanSpeed.AUTO), //
    HEAT_TIMER2("Heizen 21°", "Automatisch", "Heizen 21° 13:00", ConditionColor.ORANGE, true, HeatpumpRoofMode.HEATING, 21, HeatpumpRoofFanSpeed.AUTO), //

    FAN_AUTO("Gebläse", "Automatisch", "Gebläse+", ConditionColor.GREEN, true, HeatpumpRoofMode.FAN, 21, HeatpumpRoofFanSpeed.AUTO), //
    FAN_MIN("Gebläse", "Leise", "Gebläse-", ConditionColor.GREEN, true, HeatpumpRoofMode.FAN, 21, HeatpumpRoofFanSpeed.ONE), //
    DRY_TIMER("Trocknen", "Timer", "Trocknen", ConditionColor.GREEN, true, HeatpumpRoofMode.FAN, 21, HeatpumpRoofFanSpeed.FIVE), //

    OFF("Aus", null, "Aus", ConditionColor.DEFAULT, false, HeatpumpRoofMode.FAN, 21, HeatpumpRoofFanSpeed.ONE), //

    UNKNOWN("Unbekannt", null, "", ConditionColor.DEFAULT, false, HeatpumpRoofMode.FAN, 21, HeatpumpRoofFanSpeed.ONE), //
    ;

    private final String mode;
    private final String intensity;
    private final String shortText;
    private final ConditionColor conditionColor;
    private final boolean expectedOnOffState;
    private final HeatpumpRoofMode expectedMode;
    private final Integer expectedTemperature;
    private final HeatpumpRoofFanSpeed fanSpeed;

    HeatpumpRoofPreset(String mode, String intensity, String shortText, ConditionColor conditionColor, boolean expectedOnOffState, HeatpumpRoofMode expectedMode, Integer expectedTemperature, HeatpumpRoofFanSpeed fanSpeed) {
        this.mode = mode;
        this.intensity = intensity;
        this.conditionColor = conditionColor;
        this.shortText = shortText;
        this.expectedOnOffState = expectedOnOffState;
        this.expectedMode = expectedMode;
        this.expectedTemperature = expectedTemperature;
        this.fanSpeed = fanSpeed;
    }

}
