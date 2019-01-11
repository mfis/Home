package homecontroller.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class HeatingModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean boostActive;

	private int boostMinutesLeft;

	private BigDecimal targetTemperature;

	private Device deviceHeating;

	// Needs programs: "...Manual", "...Boost" and variable "...Temperature"
	private String programNamePrefix;

	public HeatingModel() {
		super();
	}

	public boolean isBoostActive() {
		return boostActive;
	}

	public void setBoostActive(boolean boostActive) {
		this.boostActive = boostActive;
	}

	public int getBoostMinutesLeft() {
		return boostMinutesLeft;
	}

	public void setBoostMinutesLeft(int boostMinutesLeft) {
		this.boostMinutesLeft = boostMinutesLeft;
	}

	public BigDecimal getTargetTemperature() {
		return targetTemperature;
	}

	public void setTargetTemperature(BigDecimal targetTemperature) {
		this.targetTemperature = targetTemperature;
	}

	public String getProgramNamePrefix() {
		return programNamePrefix;
	}

	public void setProgramNamePrefix(String programNamePrefix) {
		this.programNamePrefix = programNamePrefix;
	}

	public Device getDeviceHeating() {
		return deviceHeating;
	}

	public void setDeviceHeating(Device deviceHeating) {
		this.deviceHeating = deviceHeating;
	}

}
