package de.fimatas.home.library.domain.model;

import lombok.Getter;

@Getter
public enum ChargeLimit {

    _60((short)60, "60%"), //
    _70((short)70, "70%"), //
    _80((short)80, "80%"), //
    _90((short)90, "90%"), //
    MAX((short)100, "Max"), //
    ;

    private final short percentage;

    private final String caption;

    ChargeLimit(short percentage, String caption) {
        this.percentage = percentage;
        this.caption = caption;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

}
