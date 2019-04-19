package home.domain.model;

public class FrontDoorView {

	private String lastDoorbells = "";

	private String cameraStatus = "";

	private String linkLive = "";

	public String getLastDoorbells() {
		return lastDoorbells;
	}

	public void setLastDoorbells(String lastDoorbells) {
		this.lastDoorbells = lastDoorbells;
	}

	public String getCameraStatus() {
		return cameraStatus;
	}

	public void setCameraStatus(String cameraStatus) {
		this.cameraStatus = cameraStatus;
	}

	public String getLinkLive() {
		return linkLive;
	}

	public void setLinkLive(String linkLive) {
		this.linkLive = linkLive;
	}

}
