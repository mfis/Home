package homecontroller.domain.model;

import java.io.Serializable;

public class RoomClimate extends Climate implements Serializable {

	private static final long serialVersionUID = 1L;

	private HeatingModel heating = null;

	private Hint hint;

	private Device deviceHeating;

	public HeatingModel getHeating() {
		return heating;
	}

	public void setHeating(HeatingModel heating) {
		this.heating = heating;
	}

	public Hint getHint() {
		return hint;
	}

	public void setHint(Hint hint) {
		this.hint = hint;
	}

	public Device getDeviceHeating() {
		return deviceHeating;
	}

	public void setDeviceHeating(Device deviceHeating) {
		this.deviceHeating = deviceHeating;
	}
}
