package homecontroller.domain.model;

import java.io.Serializable;

public abstract class AbstractDeviceModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private Device device;

	private Type subType;

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
}
