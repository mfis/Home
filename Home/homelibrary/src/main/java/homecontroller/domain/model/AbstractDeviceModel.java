package homecontroller.domain.model;

import java.io.Serializable;

import homelibrary.homematic.model.Device;

public abstract class AbstractDeviceModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private Device device;

	private Type subType;

	private boolean busy;

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Type getSubType() {
		return subType;
	}

	public void setSubType(Type subType) {
		this.subType = subType;
	}

	public boolean isBusy() {
		return busy;
	}

	public void setBusy(boolean busy) {
		this.busy = busy;
	}
}
