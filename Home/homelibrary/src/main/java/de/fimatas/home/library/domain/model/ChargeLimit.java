package de.fimatas.home.library.domain.model;

public enum ChargeLimit {

    MEDIUM(60), //
    NEARFULL(85), //
    MAX(100), //
    ;

    private final Integer percentage;

    private ChargeLimit(Integer percentage) {
        this.percentage = percentage;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

    public Integer getPercentage() {
        return percentage;
    }


}
