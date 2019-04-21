package homecontroller.domain.model;

import java.io.Serializable;

public class FrontDoor implements Serializable {

	private static final long serialVersionUID = 1L;

	public FrontDoor() {
		super();
	}

	private long timestampLastDoorbell;

	public long getTimestampLastDoorbell() {
		return timestampLastDoorbell;
	}

	public void setTimestampLastDoorbell(long timestampLastDoorbell) {
		this.timestampLastDoorbell = timestampLastDoorbell;
	}

}
