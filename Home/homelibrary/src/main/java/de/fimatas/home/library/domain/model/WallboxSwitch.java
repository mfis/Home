package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class WallboxSwitch extends Switch implements Serializable {

    private static final long serialVersionUID = 1L;

    public WallboxSwitch() {
        super();
    }

    private boolean timer;

    public boolean isTimer() {
        return timer;
    }

    public void setTimer(boolean timer) {
        this.timer = timer;
    }
}
