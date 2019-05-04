package homecontroller.domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CameraModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<Device, CameraPicture> livePictures = new HashMap<>();

	private Map<Device, CameraPicture> eventPictures = new HashMap<>();

	public Map<Device, CameraPicture> getLivePictures() {
		return livePictures;
	}

	public void setLivePictures(Map<Device, CameraPicture> livePictures) {
		this.livePictures = livePictures;
	}

	public Map<Device, CameraPicture> getEventPictures() {
		return eventPictures;
	}

	public void setEventPictures(Map<Device, CameraPicture> eventPictures) {
		this.eventPictures = eventPictures;
	}

	// ----------

}
