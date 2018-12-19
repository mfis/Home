package homecontroller.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Climate implements Serializable {

	private static final long serialVersionUID = 1L;

	private BigDecimal temperature;

	private BigDecimal temperatureReference;

	private long temperatureReferenceDateTime;

	private Tendency temperatureTendency;

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

	public Tendency getTemperatureTendency() {
		return temperatureTendency;
	}

	public void setTemperatureTendency(Tendency temperatureTendency) {
		this.temperatureTendency = temperatureTendency;
	}

	public BigDecimal getTemperatureReference() {
		return temperatureReference;
	}

	public void setTemperatureReference(BigDecimal temperatureReference) {
		this.temperatureReference = temperatureReference;
	}

	public long getTemperatureReferenceDateTime() {
		return temperatureReferenceDateTime;
	}

	public void setTemperatureReferenceDateTime(long temperatureReferenceDateTime) {
		this.temperatureReferenceDateTime = temperatureReferenceDateTime;
	}

}
