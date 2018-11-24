package homecontroller.domain.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class RoomClimate extends Climate implements Serializable {

	private static final long serialVersionUID = 1L;

	private HeatingModel heating = null;

	private List<Hint> hints = new LinkedList<Hint>();

	private Device deviceHeating;

	public HeatingModel getHeating() {
		return heating;
	}

	public void setHeating(HeatingModel heating) {
		this.heating = heating;
	}

	public Device getDeviceHeating() {
		return deviceHeating;
	}

	public void setDeviceHeating(Device deviceHeating) {
		this.deviceHeating = deviceHeating;
	}

	public List<Hint> getHints() {
		return hints;
	}

	public void setHints(List<Hint> hints) {
		this.hints = hints;
	}
}
