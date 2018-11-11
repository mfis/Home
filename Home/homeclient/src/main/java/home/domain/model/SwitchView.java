package home.domain.model;

public class SwitchView {

	private String name = "";
	private String state = "";
	private String label = "";
	private String link = "#";
	private String linkAuto = "#";
	private String linkManual = "#";
	private String autoInfoText = "";
	private String icon = "";
	private String id = "";

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

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

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

}
