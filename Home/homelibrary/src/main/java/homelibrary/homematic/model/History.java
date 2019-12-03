package homelibrary.homematic.model;

public enum History {

	BAD_TEMPERATURE(HomematicCommand.read(Device.THERMOSTAT_BAD, Datapoint.ACTUAL_TEMPERATURE),
			HistoryStrategy.AVG), //

	KINDERZIMMER_TEMPERATURE(
			HomematicCommand.read(Device.THERMOMETER_KINDERZIMMER, Datapoint.ACTUAL_TEMPERATURE),
			HistoryStrategy.AVG), //
	KINDERZIMMER_HUMIDITY(HomematicCommand.read(Device.THERMOMETER_KINDERZIMMER, Datapoint.HUMIDITY),
			HistoryStrategy.AVG), //

	WOHNZIMMER_TEMPERATURE(HomematicCommand.read(Device.THERMOMETER_WOHNZIMMER, Datapoint.ACTUAL_TEMPERATURE),
			HistoryStrategy.AVG), //
	WOHNZIMMER_HUMIDITY(HomematicCommand.read(Device.THERMOMETER_WOHNZIMMER, Datapoint.HUMIDITY),
			HistoryStrategy.AVG), //

	SCHLAFZIMMER_TEMPERATURE(
			HomematicCommand.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.ACTUAL_TEMPERATURE),
			HistoryStrategy.AVG), //
	SCHLAFZIMMER_HUMIDITY(HomematicCommand.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.HUMIDITY),
			HistoryStrategy.AVG), //

	WASCHKUECHE_TEMPERATURE(
			HomematicCommand.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.ACTUAL_TEMPERATURE),
			HistoryStrategy.AVG), //
	WASCHKUECHE_HUMIDITY(HomematicCommand.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.HUMIDITY),
			HistoryStrategy.AVG), //

	DRAUSSEN_TEMPERATURE(HomematicCommand.read(Device.AUSSENTEMPERATUR, Datapoint.VALUE),
			HistoryStrategy.AVG), //

	STROM_ZAEHLERSTAND(HomematicCommand.read(Device.STROMZAEHLER, Datapoint.ENERGY_COUNTER),
			HistoryStrategy.MAX), //
	;

	private HomematicCommand command;

	private HistoryStrategy strategy;

	private History(HomematicCommand command, HistoryStrategy strategy) {
		this.command = command;
		this.strategy = strategy;
	}

	public HistoryStrategy getStrategy() {
		return strategy;
	}

	public HomematicCommand getCommand() {
		return command;
	}
}
