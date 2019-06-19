package home.domain.model;

public class PowerView extends View {

	private String state = "";
	private String tendencyIcon = "";
	private String description = "";

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
