package de.fimatas.home.library.domain.model;

import java.io.Serializable;

import de.fimatas.home.library.homematic.model.Device;

public class FrontDoor implements Serializable {

	private static final long serialVersionUID = 1L;

	public FrontDoor() {
		super();
	}

	private Long timestampLastDoorbell;
	
	private boolean lockState;
	
	private boolean lockStateUncertain;

	private Boolean lockAutomation;

	private String lockAutomationInfoText;

	private Device deviceDoorBell;

	private Device deviceDoorBellHistory;

	private Device deviceCamera;
	
	private Device deviceLock;

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

	public Device getDeviceLock() {
		return deviceLock;
	}

	public void setDeviceLock(Device deviceLock) {
		this.deviceLock = deviceLock;
	}

}
