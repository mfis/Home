package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class WindowSensor extends AbstractDeviceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public WindowSensor() {
        super();
    }

    private boolean state;

    private Long stateTimestamp;

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public Long getStateTimestamp() {
        return stateTimestamp;
    }

    public void setStateTimestamp(Long stateTimestamp) {
        this.stateTimestamp = stateTimestamp;
    }

}
