package homecontroller.domain.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class RoomClimate extends Climate implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<Hint> hints = new LinkedList<>();

	public List<Hint> getHints() {
		return hints;
	}

	public void setHints(List<Hint> hints) {
		this.hints = hints;
	}
}
