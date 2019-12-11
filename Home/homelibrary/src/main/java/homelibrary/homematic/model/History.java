package homelibrary.homematic.model;

public enum History {

	// BAD_TEMPERATURE(HomematicCommand.read(Device.THERMOSTAT_BAD,
	// Datapoint.ACTUAL_TEMPERATURE),
	// HistoryStrategy.AVG, 1), //

	KINDERZIMMER_TEMPERATURE(
			HomematicCommand.read(Device.THERMOMETER_KINDERZIMMER, Datapoint.ACTUAL_TEMPERATURE),
			HistoryStrategy.AVG, 1), //
	KINDERZIMMER_HUMIDITY(HomematicCommand.read(Device.THERMOMETER_KINDERZIMMER, Datapoint.HUMIDITY),
			HistoryStrategy.AVG, 2), //

	// WOHNZIMMER_TEMPERATURE(HomematicCommand.read(Device.THERMOMETER_WOHNZIMMER,
	// Datapoint.ACTUAL_TEMPERATURE),
	// HistoryStrategy.AVG, 1), //
	// WOHNZIMMER_HUMIDITY(HomematicCommand.read(Device.THERMOMETER_WOHNZIMMER,
	// Datapoint.HUMIDITY),
	// HistoryStrategy.AVG, 1), //

	SCHLAFZIMMER_TEMPERATURE(
			HomematicCommand.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.ACTUAL_TEMPERATURE),
			HistoryStrategy.AVG, 1), //
	SCHLAFZIMMER_HUMIDITY(HomematicCommand.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.HUMIDITY),
			HistoryStrategy.AVG, 2), //

	WASCHKUECHE_TEMPERATURE(
			HomematicCommand.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.ACTUAL_TEMPERATURE),
			HistoryStrategy.AVG, 1), //
	WASCHKUECHE_HUMIDITY(HomematicCommand.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.HUMIDITY),
			HistoryStrategy.AVG, 2), //

	DRAUSSEN_TEMPERATURE(HomematicCommand.read(Device.AUSSENTEMPERATUR, Datapoint.VALUE), HistoryStrategy.AVG,
			1), //

	STROM_ZAEHLERSTAND(HomematicCommand.read(Device.STROMZAEHLER, Datapoint.ENERGY_COUNTER),
			HistoryStrategy.MAX, 1000), //
	;

	private HomematicCommand command;

	private HistoryStrategy strategy;

	private int valueDifferenceToSave;

	private History(HomematicCommand command, HistoryStrategy strategy, int valueDifferenceToSave) {
		this.command = command;
		this.strategy = strategy;
		this.valueDifferenceToSave = valueDifferenceToSave;
	}

	public HistoryStrategy getStrategy() {
		return strategy;
	}

	public HomematicCommand getCommand() {
		return command;
	}

	public int getValueDifferenceToSave() {
		return valueDifferenceToSave;
	}
}
