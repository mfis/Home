package de.fimatas.home.library.domain.model;

import java.io.Serializable;

import de.fimatas.home.library.homematic.model.Device;

public class OutdoorClimate extends Climate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Intensity sunBeamIntensity;

    private Device base;

    public Intensity getSunBeamIntensity() {
        return sunBeamIntensity;
    }

    public void setSunBeamIntensity(Intensity sunBeamIntensity) {
        this.sunBeamIntensity = sunBeamIntensity;
    }

    public Device getBase() {
        return base;
    }

    public void setBase(Device base) {
        this.base = base;
    }

}
