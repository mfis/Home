package de.fimatas.home.client.domain.model;

public abstract class View {

	private String id = "";
	private String state = "";
	private String stateSuffix = "";
	private String icon = "";
	private String name = "";
	private String place = "";
	private String historyKey = "";

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

	public String getHistoryKey() {
		return historyKey;
	}

	public void setHistoryKey(String historyKey) {
		this.historyKey = historyKey;
	}

	public String getPlace() {
		return place;
	}

	public void setPlace(String place) {
		this.place = place;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStateSuffix() {
		return stateSuffix;
	}

	public void setStateSuffix(String stateSuffix) {
		this.stateSuffix = stateSuffix;
	}

}
