package home.domain.model;

public class PowerView {

	private String name = "";
	private String state = "";
	private String icon = "";
	private String tendencyIcon = "";
	private String id = "";
	private String description = "";

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

	public String getTendencyIcon() {
		return tendencyIcon;
	}

	public void setTendencyIcon(String tendencyIcon) {
		this.tendencyIcon = tendencyIcon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
