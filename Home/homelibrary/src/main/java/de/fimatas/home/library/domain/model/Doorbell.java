package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class Doorbell extends AbstractDeviceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public Doorbell() {
        super();
    }

    private Long timestampLastDoorbell;

    public Long getTimestampLastDoorbell() {
        return timestampLastDoorbell;
    }

    public void setTimestampLastDoorbell(Long timestampLastDoorbell) {
        this.timestampLastDoorbell = timestampLastDoorbell;
    }

}
