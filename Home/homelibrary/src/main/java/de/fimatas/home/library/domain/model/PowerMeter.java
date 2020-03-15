package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class PowerMeter extends AbstractDeviceModel implements Serializable {

	private static final long serialVersionUID = 1L;

	public PowerMeter() {
		super();
	}

	private ValueWithTendency<BigDecimal> actualConsumption;

	public ValueWithTendency<BigDecimal> getActualConsumption() {
		return actualConsumption;
	}

	public void setActualConsumption(ValueWithTendency<BigDecimal> actualConsumption) {
		this.actualConsumption = actualConsumption;
	}

}
