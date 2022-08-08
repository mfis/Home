package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;

public enum HeatpumpPreset {

    COOL_AUTO("Kühlen", "Automatisch", "Kühlen+", ConditionColor.BLUE), //
    COOL_MIN("Kühlen", "Leise", "Kühlen-", ConditionColor.BLUE), //
    HEAT_AUTO("Heizen", "Automatisch", "Heizen+", ConditionColor.ORANGE), //
    HEAT_MIN("Heizen", "Leise", "Heizen-", ConditionColor.ORANGE), //
    FAN_AUTO("Gebläse", "Automatisch", "Gebläse+", ConditionColor.GREEN), //
    FAN_MIN("Gebläse", "Leise", "Gebläse-", ConditionColor.GREEN), //
    DRY_TIMER("Trocknen", "Timer", "Trocknen", ConditionColor.GREEN), //
    OFF("Aus", null, "Aus", ConditionColor.GRAY), //
    UNKNOWN("Unbekannt", null, "", ConditionColor.RED), //
    ;

    private final String mode;
    private final String intensity;

    private final String shortText;

    private final ConditionColor conditionColor;

    HeatpumpPreset(String mode, String intensity, String shortText, ConditionColor conditionColor) {
        this.mode = mode;
        this.intensity = intensity;
        this.conditionColor = conditionColor;
        this.shortText = shortText;
    }

    public String getMode() {
        return mode;
    }

    public String getIntensity() {
        return intensity;
    }

    public ConditionColor getConditionColor() {return conditionColor;}

    public String getShortText(){
        return shortText;
    }
}
