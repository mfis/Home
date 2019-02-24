package homecontroller.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Climate extends AbstractDeviceModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private ValueWithTendency<BigDecimal> temperature;

	private ValueWithTendency<BigDecimal> humidity;

	public ValueWithTendency<BigDecimal> getTemperature() {
		return temperature;
	}

	public void setTemperature(ValueWithTendency<BigDecimal> temperature) {
		this.temperature = temperature;
	}

	public ValueWithTendency<BigDecimal> getHumidity() {
		return humidity;
	}

	public void setHumidity(ValueWithTendency<BigDecimal> humidity) {
		this.humidity = humidity;
	}

}
