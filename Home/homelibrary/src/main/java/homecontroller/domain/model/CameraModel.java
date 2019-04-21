package homecontroller.domain.model;

import java.util.HashMap;
import java.util.Map;

public class CameraModel {

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
