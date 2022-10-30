package de.fimatas.home.library.domain.model;

public enum ElectricVehicle {

    EUP("e-Up", false), //
    OTHER("Anderes", true), //
    ;

    private final String caption;


    private final boolean other;

    private ElectricVehicle(String caption, boolean other) {
        this.caption = caption;
        this.other = other;
    }

    public String getCaption() {
        return caption;
    }

    public boolean isOther() {
        return other;
    }

}
