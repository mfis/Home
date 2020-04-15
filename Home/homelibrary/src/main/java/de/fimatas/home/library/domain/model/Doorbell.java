package de.fimatas.home.library.domain.model;

import java.io.Serializable;

import de.fimatas.home.library.homematic.model.Device;

public class Doorbell extends AbstractDeviceModel implements Serializable {

	private static final long serialVersionUID = 1L;

	public Doorbell() {
		super();
	}

	private Long timestampLastDoorbell;

	private Device historyDevice;

	public Long getTimestampLastDoorbell() {
		return timestampLastDoorbell;
	}

	public void setTimestampLastDoorbell(Long timestampLastDoorbell) {
		this.timestampLastDoorbell = timestampLastDoorbell;
	}

	public Device getHistoryDevice() {
		return historyDevice;
	}

	public void setHistoryDevice(Device historyDevice) {
		this.historyDevice = historyDevice;
	}

}
