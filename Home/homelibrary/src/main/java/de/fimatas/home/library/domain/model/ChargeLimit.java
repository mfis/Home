package de.fimatas.home.library.domain.model;

import lombok.Getter;

@Getter
public enum ChargeLimit {

    _25((short)25, "25%"), //
    _40((short)40, "40%"), //
    _50((short)50, "50%"), //
    _80((short)80, "80%"), //
    _85((short)85, "85%"), //
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
