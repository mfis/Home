package home.domain.model;

public class PowerHistoryEntry {

	private String key;

	private String value;

	private String colorClass;

	private String calculated;

	private String previousYearCompare;

	private String previousYearCompareClass;

	private String collapse;

	public PowerHistoryEntry() {
		super();
		colorClass = "";
		calculated = "";
		previousYearCompare = "";
		collapse = "";
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getColorClass() {
		return colorClass;
	}

	public void setColorClass(String colorClass) {
		this.colorClass = colorClass;
	}

	public String getCalculated() {
		return calculated;
	}

	public void setCalculated(String calculated) {
		this.calculated = calculated;
	}

	public String getCollapse() {
		return collapse;
	}

	public void setCollapse(String collapse) {
		this.collapse = collapse;
	}

	public String getPreviousYearCompare() {
		return previousYearCompare;
	}

	public void setPreviousYearCompare(String previousYearCompare) {
		this.previousYearCompare = previousYearCompare;
	}

	public String getPreviousYearCompareClass() {
		return previousYearCompareClass;
	}

	public void setPreviousYearCompareClass(String previousYearCompareClass) {
		this.previousYearCompareClass = previousYearCompareClass;
	}
}
