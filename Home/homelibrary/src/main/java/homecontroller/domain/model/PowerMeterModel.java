package homecontroller.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class PowerMeterModel implements Serializable {

	private static final long serialVersionUID = 1L;

	public PowerMeterModel() {
		super();
	}

	private ValueWithTendency<BigDecimal> actualConsumption;

	private Device device;

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public ValueWithTendency<BigDecimal> getActualConsumption() {
		return actualConsumption;
	}

	public void setActualConsumption(ValueWithTendency<BigDecimal> actualConsumption) {
		this.actualConsumption = actualConsumption;
	}

}
