package homecontroller.domain.model;

public enum Datapoint {

	TEMPERATURE(ValueFormat.DEC), //
	ACTUAL_TEMPERATURE(ValueFormat.DEC), //
	HUMIDITY(ValueFormat.DEC), //
	CONTROL_MODE(ValueFormat.DEC), //
	BOOST_STATE(ValueFormat.DEC), //
	SET_TEMPERATURE(ValueFormat.DEC), //
	STATE(ValueFormat.DEC), //
	POWER(ValueFormat.DEC), //
	LOWBAT(ValueFormat.DEC), //
	LOW_BAT(ValueFormat.DEC), //
	ENERGY_COUNTER(ValueFormat.DEC), //
	VALUE(ValueFormat.DEC), //
	;

	private ValueFormat valueFormat;

	private Datapoint(ValueFormat valueFormat) {
		this.valueFormat = valueFormat;
	}

	public String getHistorianPrefix() {
		return valueFormat.getHistorianPrefix();
	}

	private enum ValueFormat {

		DEC("D"), CHAR("C");

		private String historianPrefix;

		private ValueFormat(String historianPrefix) {
			this.historianPrefix = historianPrefix;
		}

		public String getHistorianPrefix() {
			return historianPrefix;
		}
	}
}
