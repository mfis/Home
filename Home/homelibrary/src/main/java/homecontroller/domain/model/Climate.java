package homecontroller.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Climate implements Serializable {

	private static final long serialVersionUID = 1L;

	private BigDecimal temperature;

	private BigDecimal humidity;

	private String placeName;

	private Device deviceThermometer;

	public BigDecimal getTemperature() {
		return temperature;
	}

	public void setTemperature(BigDecimal temperature) {
		this.temperature = temperature;
	}

	public BigDecimal getHumidity() {
		return humidity;
	}

	public void setHumidity(BigDecimal humidity) {
		this.humidity = humidity;
	}

	public String getPlaceName() {
		return placeName;
	}

	public void setPlaceName(String placeName) {
		this.placeName = placeName;
	}

	public Device getDeviceThermometer() {
		return deviceThermometer;
	}

	public void setDeviceThermometer(Device deviceThermometer) {
		this.deviceThermometer = deviceThermometer;
	}

}
