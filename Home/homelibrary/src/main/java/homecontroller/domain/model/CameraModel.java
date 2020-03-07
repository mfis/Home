package homecontroller.domain.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CameraModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private CameraPicture livePicture;

	private List<CameraPicture> eventPictures;

	public CameraModel() {
		eventPictures = new LinkedList<>();
	}

	public CameraPicture getLivePicture() {
		return livePicture;
	}

	public void setLivePicture(CameraPicture livePicture) {
		this.livePicture = livePicture;
	}

	public List<CameraPicture> getEventPictures() {
		return eventPictures;
	}

	public boolean cleanUp() {

		boolean changed = false;

		if (eventPictures == null) {
			return changed;
		}

		while (eventPictures.size() > 5) {
			eventPictures.remove(0);
			changed = true;
		}

		Set<CameraPicture> toRemove = new HashSet<>();
		for (CameraPicture cameraPicture : eventPictures) {
			long timestampDiff = Math.abs(cameraPicture.getTimestamp() - System.currentTimeMillis());
			long diffHours = timestampDiff / 1000 / 60 / 60;
			if (diffHours > 72) {
				toRemove.add(cameraPicture);
			}
		}
		for (CameraPicture cameraPictureToRemove : toRemove) {
			eventPictures.remove(cameraPictureToRemove);
			changed = true;
		}

		return changed;
	}

	public void setEventPictures(List<CameraPicture> eventPictures) {
		this.eventPictures = eventPictures;
	}

}
