package home.domain;

public class PowerHistoryEntry {

	private String key;

	private String value;

	private String colorClass;

	private String calculated;

	public PowerHistoryEntry() {
		super();
		colorClass = "";
		calculated = "";
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
}
