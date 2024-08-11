package de.fimatas.home.library.domain.model;

import lombok.Getter;

@Getter
public enum PvBatteryMinCharge {

    LOW(15, 5, "15% (5%)"), //
    MEDIUM(30, 20, "30% (20%)"), //
    HIGH(60, 50, "60% (50%)"), //
    FULL(95, 85, "95% (85%)"), //
    ;

    private final int percentageSwitchOn;

    private final int percentageSwitchOff;

    private final String caption;

    public static PvBatteryMinCharge getLowest(){
        return LOW;
    }

    PvBatteryMinCharge(int percentageSwitchOn, int percentageSwitchOff, String caption) {
        this.percentageSwitchOn = percentageSwitchOn;
        this.percentageSwitchOff = percentageSwitchOff;
        this.caption = caption;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

}
