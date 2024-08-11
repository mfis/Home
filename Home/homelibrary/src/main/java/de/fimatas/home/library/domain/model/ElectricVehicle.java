package de.fimatas.home.library.domain.model;

import lombok.Getter;

@Getter
public enum ElectricVehicle {

    EUP("e-Up", false), //
    OTHER("Anderes", true), //
    ;

    private final String caption;


    private final boolean other;

    ElectricVehicle(String caption, boolean other) {
        this.caption = caption;
        this.other = other;
        if(this.name().length()>8) throw new IllegalArgumentException("name too long: " + this.name());
    }

}
