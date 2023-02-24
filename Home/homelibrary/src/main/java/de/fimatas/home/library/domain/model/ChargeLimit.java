package de.fimatas.home.library.domain.model;

public enum ChargeLimit {

    MEDIUM((short)60), //
    NEARFULL((short)85), //
    MAX((short)100), //
    ;

    private final short percentage;

    private ChargeLimit(short percentage) {
        this.percentage = percentage;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

    public short getPercentage() {
        return percentage;
    }


}
