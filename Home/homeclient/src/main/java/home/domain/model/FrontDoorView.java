package home.domain.model;

public class FrontDoorView {

	private String lastDoorbells = "";

	private String idLive = "";
	private String linkLive = "";

	private String idBell = "";
	private String linkBell = "";

	public String getLastDoorbells() {
		return lastDoorbells;
	}

	public void setLastDoorbells(String lastDoorbells) {
		this.lastDoorbells = lastDoorbells;
	}

	public String getLinkLive() {
		return linkLive;
	}

	public void setLinkLive(String linkLive) {
		this.linkLive = linkLive;
	}

	public String getIdLive() {
		return idLive;
	}

	public void setIdLive(String idLive) {
		this.idLive = idLive;
	}

	public String getIdBell() {
		return idBell;
	}

	public void setIdBell(String idBell) {
		this.idBell = idBell;
	}

	public String getLinkBell() {
		return linkBell;
	}

	public void setLinkBell(String linkBell) {
		this.linkBell = linkBell;
	}

}
