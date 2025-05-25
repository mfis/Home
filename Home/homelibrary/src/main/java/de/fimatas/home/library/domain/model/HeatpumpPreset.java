package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Getter;

@Getter
public enum HeatpumpPreset {

    COOL_AUTO("Kühlen", "Automatisch", "Kühlen+", ConditionColor.BLUE), //
    COOL_MIN("Kühlen", "Leise", "Kühlen-", ConditionColor.BLUE), //
    COOL_TIMER1("Kühlen", "Automatisch", "Kühlen 1h", ConditionColor.BLUE), //
    COOL_TIMER2("Kühlen", "Automatisch", "Kühlen 2h", ConditionColor.BLUE), //
    COOL_TIMER3("Kühlen", "Automatisch", "Kühlen 19:00", ConditionColor.BLUE), //
    HEAT_AUTO("Heizen", "Automatisch", "Heizen+", ConditionColor.ORANGE), //
    HEAT_MIN("Heizen", "Leise", "Heizen-", ConditionColor.ORANGE), //
    HEAT_TIMER1("Heizen", "Automatisch", "Heizen 1h", ConditionColor.ORANGE), //
    HEAT_TIMER2("Heizen", "Automatisch", "Heizen 2h", ConditionColor.ORANGE), //
    HEAT_TIMER3("Heizen", "Automatisch", "Heizen 13:00", ConditionColor.ORANGE), //
    FAN_AUTO("Gebläse", "Automatisch", "Gebläse+", ConditionColor.GREEN), //
    FAN_MIN("Gebläse", "Leise", "Gebläse-", ConditionColor.GREEN), //
    DRY_TIMER("Trocknen", "Timer", "Trocknen", ConditionColor.GREEN), //
    OFF("Aus", null, "Aus", ConditionColor.GRAY), //
    UNKNOWN("Unbekannt", null, "", ConditionColor.GRAY), //
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

}
