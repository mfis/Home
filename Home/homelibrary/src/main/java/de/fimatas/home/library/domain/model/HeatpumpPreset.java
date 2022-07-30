package de.fimatas.home.library.domain.model;

public enum HeatpumpPreset {

    COOL_AUTO("Kühlen", "Automatisch"), //
    COOL_MIN("Kühlen", "Leise"), //
    HEAT_AUTO("Heizen", "Automatisch"), //
    HEAT_MIN("Heizen", "Leise"), //
    FAN_AUTO("Gebläse", "Automatisch"), //
    FAN_MIN("Gebläse", "Leise"), //
    DRY_TIMER("Trocknen", "Timer"), //
    OFF("Aus", null), //
    ;

    private final String mode;
    private final String intensity;

    private HeatpumpPreset(String mode, String intensity) {
        this.mode = mode;
        this.intensity = intensity;
    }

    public String getMode() {
        return mode;
    }

    public String getIntensity() {
        return intensity;
    }
}
