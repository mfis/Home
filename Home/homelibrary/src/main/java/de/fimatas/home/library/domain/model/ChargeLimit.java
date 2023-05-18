package de.fimatas.home.library.domain.model;

public enum ChargeLimit {

    _70((short)70, "70%"), //
    _80((short)80, "80%"), //
    _85((short)85, "85%"), //
    MAX((short)100, "Max"), //
    ;

    private final short percentage;

    private final String caption;

    private ChargeLimit(short percentage, String caption) {
        this.percentage = percentage;
        this.caption = caption;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

    public short getPercentage() {
        return percentage;
    }

    public String getCaption() {
        return caption;
    }
}
