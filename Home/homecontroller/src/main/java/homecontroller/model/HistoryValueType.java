package homecontroller.model;

public enum HistoryValueType {

	SINGLE("S"), MIN("-"), MAX("+"), AVG("*");

	private String databaseKey;

	private HistoryValueType(String databaseKey) {
		this.databaseKey = databaseKey;
	}

	public String getDatabaseKey() {
		return databaseKey;
	}
}