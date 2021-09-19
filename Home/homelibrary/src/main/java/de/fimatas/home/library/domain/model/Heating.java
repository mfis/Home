package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Heating extends AbstractDeviceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean boostActive;

    private boolean autoActive;

    private boolean manualActive;

    private int boostMinutesLeft;

    private BigDecimal targetTemperature;

    public Heating() {
        super();
    }

    public boolean isBoostActive() {
        return boostActive;
    }

    public void setBoostActive(boolean boostActive) {
        this.boostActive = boostActive;
    }

    public int getBoostMinutesLeft() {
        return boostMinutesLeft;
    }

    public void setBoostMinutesLeft(int boostMinutesLeft) {
        this.boostMinutesLeft = boostMinutesLeft;
    }

    public BigDecimal getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(BigDecimal targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public boolean isAutoActive() {
        return autoActive;
    }

    public void setAutoActive(boolean autoActive) {
        this.autoActive = autoActive;
    }

    public boolean isManualActive() {
        return manualActive;
    }

    public void setManualActive(boolean manualActive) {
        this.manualActive = manualActive;
    }
}
