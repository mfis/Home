package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class RoomClimate extends Climate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean humidityWetterThanOutdoor;

    private Hints hints = new Hints();

    public Hints getHints() {
        return hints;
    }

    public void setHints(Hints hints) {
        this.hints = hints;
    }

    public Boolean getHumidityWetterThanOutdoor() {
        return humidityWetterThanOutdoor;
    }

    public void setHumidityWetterThanOutdoor(Boolean humidityWetterThanOutdoor) {
        this.humidityWetterThanOutdoor = humidityWetterThanOutdoor;
    }

}
