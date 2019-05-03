package homecontroller.domain.model;

import java.io.Serializable;

public class FrontDoor implements Serializable {

	private static final long serialVersionUID = 1L;

	public FrontDoor() {
		super();
	}

	private long timestampLastDoorbell;

	private Device deviceDoorBell;

	private Device deviceCamera;

	public long getTimestampLastDoorbell() {
		return timestampLastDoorbell;
	}

	public void setTimestampLastDoorbell(long timestampLastDoorbell) {
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

}
