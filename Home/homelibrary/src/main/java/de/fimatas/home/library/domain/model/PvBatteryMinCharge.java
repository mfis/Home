package de.fimatas.home.library.domain.model;

import lombok.Getter;

@Getter
public enum PvBatteryMinCharge {

    _00((short)0, "0%"), //
    _20((short)20, "20%"), //
    _50((short)50, "50%"), //
    _90((short)90, "90%"), //
    ;

    private final short percentage;

    private final String caption;

    PvBatteryMinCharge(short percentage, String caption) {
        this.percentage = percentage;
        this.caption = caption;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

}
