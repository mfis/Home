package de.fimatas.home.library.domain.model;

public enum EvChargePoint {

    WALLBOX1(1)
    ;

    private final int number;

    EvChargePoint(int number){
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
}
