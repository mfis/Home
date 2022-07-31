package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;

public enum HeatpumpPreset {

    COOL_AUTO("Kühlen", "Automatisch", ConditionColor.BLUE), //
    COOL_MIN("Kühlen", "Leise", ConditionColor.BLUE), //
    HEAT_AUTO("Heizen", "Automatisch", ConditionColor.ORANGE), //
    HEAT_MIN("Heizen", "Leise", ConditionColor.ORANGE), //
    FAN_AUTO("Gebläse", "Automatisch", ConditionColor.GREEN), //
    FAN_MIN("Gebläse", "Leise", ConditionColor.GREEN), //
    DRY_TIMER("Trocknen", "Timer", ConditionColor.GREEN), //
    OFF("Aus", null, ConditionColor.GRAY), //
    UNKNOWN("Unbekannt", null, ConditionColor.RED), //
    ;

    private final String mode;
    private final String intensity;

    private final ConditionColor conditionColor;

    private HeatpumpPreset(String mode, String intensity, ConditionColor conditionColor) {
        this.mode = mode;
        this.intensity = intensity;
        this.conditionColor = conditionColor;
    }

    public String getMode() {
        return mode;
    }

    public String getIntensity() {
        return intensity;
    }

    public ConditionColor getConditionColor() {return conditionColor;}
}
