package de.fimatas.home.client.domain.model;

public class LockView extends View {

	private String linkAuto = "#";
	private String linkManual = "#";
	private String autoInfoText = "";

	public String getLinkAuto() {
		return linkAuto;
	}

	public void setLinkAuto(String linkAuto) {
		this.linkAuto = linkAuto;
	}

	public String getLinkManual() {
		return linkManual;
	}

	public void setLinkManual(String linkManual) {
		this.linkManual = linkManual;
	}

	public String getAutoInfoText() {
		return autoInfoText;
	}

	public void setAutoInfoText(String autoInfoText) {
		this.autoInfoText = autoInfoText;
	}

}
