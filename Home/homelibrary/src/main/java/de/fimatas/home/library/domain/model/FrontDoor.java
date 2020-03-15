package de.fimatas.home.library.domain.model;

import java.io.Serializable;

import de.fimatas.home.library.homematic.model.Device;

public class FrontDoor implements Serializable {

	private static final long serialVersionUID = 1L;

	public FrontDoor() {
		super();
	}

	private Long timestampLastDoorbell;

	private Device deviceDoorBell;

	private Device deviceDoorBellHistory;

	private Device deviceCamera;

	public Long getTimestampLastDoorbell() {
		return timestampLastDoorbell;
	}

	public void setTimestampLastDoorbell(Long timestampLastDoorbell) {
		this.timestampLastDoorbell = timestampLastDoorbell;
	}

	public Device getDeviceDoorBell() {
		return deviceDoorBell;
	}

	public void setDeviceDoorBell(Device deviceDoorBell) {
		this.deviceDoorBell = deviceDoorBell;
	}

	public Device getDeviceCamera() {
		return deviceCamera;
	}

	public void setDeviceCamera(Device deviceCamera) {
		this.deviceCamera = deviceCamera;
	}

	public Device getDeviceDoorBellHistory() {
		return deviceDoorBellHistory;
	}

	public void setDeviceDoorBellHistory(Device deviceDoorBellHistory) {
		this.deviceDoorBellHistory = deviceDoorBellHistory;
	}

}