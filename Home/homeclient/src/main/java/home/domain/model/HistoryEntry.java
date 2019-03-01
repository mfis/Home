package home.domain.model;

public class HistoryEntry {

	private String lineOneLabel;

	private String lineOneValue;
	
	private String lineOneValueIcon;

	private String colorClass;

	private String lineTwoLabel;
	
	private String lineTwoValue;
	
	private String lineTwoValueIcon;

	private String badgeLabel;
	
	private String badgeValue;

	private String badgeClass;

	private String collapse;

	public HistoryEntry() {
		super();
		lineOneLabel = "";
		lineOneValue = "";
		lineOneValueIcon = "";
		colorClass = "";
		lineTwoLabel = "";
		lineTwoValue = "";
		lineTwoValueIcon = "";
		badgeLabel = "";
		badgeValue = "";
		badgeClass = "";
		collapse = "";
	}

	public String getLineOneValueIcon() {
		return lineOneValueIcon;
	}

	public void setLineOneValueIcon(String lineOneValueIcon) {
		this.lineOneValueIcon = lineOneValueIcon;
	}

	public String getLineTwoValueIcon() {
		return lineTwoValueIcon;
	}

	public void setLineTwoValueIcon(String lineTwoValueIcon) {
		this.lineTwoValueIcon = lineTwoValueIcon;
	}

	public String getLineOneLabel() {
		return lineOneLabel;
	}

	public void setLineOneLabel(String lineOneLabel) {
		this.lineOneLabel = lineOneLabel;
	}

	public String getLineOneValue() {
		return lineOneValue;
	}

	public void setLineOneValue(String lineOneValue) {
		this.lineOneValue = lineOneValue;
	}

	public String getColorClass() {
		return colorClass;
	}

	public void setColorClass(String colorClass) {
		this.colorClass = colorClass;
	}

	public String getLineTwoLabel() {
		return lineTwoLabel;
	}

	public void setLineTwoLabel(String lineTwoLabel) {
		this.lineTwoLabel = lineTwoLabel;
	}

	public String getLineTwoValue() {
		return lineTwoValue;
	}

	public void setLineTwoValue(String lineTwoValue) {
		this.lineTwoValue = lineTwoValue;
	}

	public String getBadgeLabel() {
		return badgeLabel;
	}

	public void setBadgeLabel(String badgeLabel) {
		this.badgeLabel = badgeLabel;
	}

	public String getBadgeValue() {
		return badgeValue;
	}

	public void setBadgeValue(String badgeValue) {
		this.badgeValue = badgeValue;
	}

	public String getBadgeClass() {
		return badgeClass;
	}

	public void setBadgeClass(String badgeClass) {
		this.badgeClass = badgeClass;
	}

	public String getCollapse() {
		return collapse;
	}

	public void setCollapse(String collapse) {
		this.collapse = collapse;
	}

}
