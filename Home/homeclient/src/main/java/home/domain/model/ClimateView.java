package home.domain.model;

public class ClimateView {

	private String id = "";
	private String name = "";
	private String postfix = "";
	private String state = "";
	private String statePostfixIcon = "";
	private String colorClass = "secondary";
	private String linkBoost = "";
	private String linkManual = "";
	private String targetTemp = "";
	private String icon = "";
	private String heatericon = "";
	private String hint = "";

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

	public String getColorClass() {
		return colorClass;
	}

	public void setColorClass(String colorClass) {
		this.colorClass = colorClass;
	}

	public String getLinkBoost() {
		return linkBoost;
	}

	public void setLinkBoost(String linkBoost) {
		this.linkBoost = linkBoost;
	}

	public String getLinkManual() {
		return linkManual;
	}

	public void setLinkManual(String linkManual) {
		this.linkManual = linkManual;
	}

	public String getTargetTemp() {
		return targetTemp;
	}

	public void setTargetTemp(String targetTemp) {
		this.targetTemp = targetTemp;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getHeatericon() {
		return heatericon;
	}

	public void setHeatericon(String heatericon) {
		this.heatericon = heatericon;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public String getPostfix() {
		return postfix;
	}

	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}

	public String getStatePostfixIcon() {
		return statePostfixIcon;
	}

	public void setStatePostfixIcon(String statePostfixIcon) {
		this.statePostfixIcon = statePostfixIcon;
	}

}
