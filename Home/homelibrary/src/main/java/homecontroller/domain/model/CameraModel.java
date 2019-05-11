package homecontroller.domain.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class CameraModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private CameraPicture livePicture;

	private List<CameraPicture> eventPictures = new LinkedList<>();

	public CameraPicture getLivePicture() {
		return livePicture;
	}

	public void setLivePicture(CameraPicture livePicture) {
		this.livePicture = livePicture;
	}

	public List<CameraPicture> getEventPictures() {
		return eventPictures;
	}

	public void setEventPictures(List<CameraPicture> eventPictures) {
		this.eventPictures = eventPictures;
	}

}
