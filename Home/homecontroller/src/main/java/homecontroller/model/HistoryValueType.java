package homecontroller.model;

public enum HistoryValueType {

	SINGLE("S"), MIN("-"), MAX("+"), AVG("*");

	private String databaseKey;

	private HistoryValueType(String databaseKey) {
		this.databaseKey = databaseKey;
	}

	public static HistoryValueType fromKey(String key) {
		for (HistoryValueType type : values()) {
			if (type.getDatabaseKey().equals(key)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown HistoryValueType key: " + key);
	}

	public String getDatabaseKey() {
		return databaseKey;
	}
}