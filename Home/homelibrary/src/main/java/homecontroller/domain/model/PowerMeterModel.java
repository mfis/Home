package homecontroller.domain.model;

import java.io.Serializable;

public class PowerMeterModel implements Serializable {

	private static final long serialVersionUID = 1L;

	public PowerMeterModel() {
		super();
	}

	private int actualConsumption;

	private Device device;

	public int getActualConsumption() {
		return actualConsumption;
	}

	public void setActualConsumption(int actualConsumption) {
		this.actualConsumption = actualConsumption;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

}
