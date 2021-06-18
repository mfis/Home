package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Doorlock extends AbstractDeviceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public Doorlock() {
        super();
    }

    private boolean open;

    private boolean lockState;

    private boolean lockStateUncertain;

    private Boolean lockAutomation;

    private Boolean lockAutomationEvent;

    private String lockAutomationInfoText;

    private LocalDateTime busyTimestamp;

    public boolean isLockState() {
        return lockState;
    }

    public void setLockState(boolean lockState) {
        this.lockState = lockState;
    }

    public boolean isLockStateUncertain() {
        return lockStateUncertain;
    }

    public void setLockStateUncertain(boolean lockStateUncertain) {
        this.lockStateUncertain = lockStateUncertain;
    }

    public Boolean getLockAutomation() {
        return lockAutomation;
    }

    public void setLockAutomation(Boolean lockAutomation) {
        this.lockAutomation = lockAutomation;
    }

    public String getLockAutomationInfoText() {
        return lockAutomationInfoText;
    }

    public void setLockAutomationInfoText(String lockAutomationInfoText) {
        this.lockAutomationInfoText = lockAutomationInfoText;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Boolean getLockAutomationEvent() {
        return lockAutomationEvent;
    }

    public void setLockAutomationEvent(Boolean lockAutomationEvent) {
        this.lockAutomationEvent = lockAutomationEvent;
    }

    public LocalDateTime getBusyTimestamp() {
        return busyTimestamp;
    }

    public void setBusyTimestamp(LocalDateTime busyTimestamp) {
        this.busyTimestamp = busyTimestamp;
    }
}
