package de.fimatas.home.library.domain.model;

import lombok.Getter;

@Getter
public enum PvBatteryMinCharge {

    _10((short)10, "10%"), //
    _20((short)20, "20%"), //
    _50((short)50, "50%"), //
    _90((short)90, "90%"), //
    ;

    private final short percentage;

    private final String caption;

    public static PvBatteryMinCharge getLowest(){
        return _10;
    }

    PvBatteryMinCharge(short percentage, String caption) {
        this.percentage = percentage;
        this.caption = caption;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

}
