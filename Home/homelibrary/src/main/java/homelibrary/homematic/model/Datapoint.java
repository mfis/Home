package homelibrary.homematic.model;

import java.util.Arrays;
import java.util.List;

public enum Datapoint {

	TEMPERATURE(HomematicValueFormat.DEC, null), //
	ACTUAL_TEMPERATURE(HomematicValueFormat.DEC, null), //
	HUMIDITY(HomematicValueFormat.DEC, null), //
	CONTROL_MODE(HomematicValueFormat.DEC, null), //
	BOOST_STATE(HomematicValueFormat.DEC, null), //
	SET_TEMPERATURE(HomematicValueFormat.DEC, null), //
	STATE(HomematicValueFormat.DEC, null), //
	POWER(HomematicValueFormat.DEC, null), //
	LOWBAT(HomematicValueFormat.DEC, 0), //
	LOW_BAT(HomematicValueFormat.DEC, 0), //
	ENERGY_COUNTER(HomematicValueFormat.DEC, null), //
	VALUE(HomematicValueFormat.DEC, null), //
	PRESS_SHORT(HomematicValueFormat.DEC, null), //
	SYSVAR_DUMMY(null, null), //
	;

	protected static final List<Datapoint> LIST_THERMOSTAT_HM = Arrays.asList(Datapoint.ACTUAL_TEMPERATURE,
			Datapoint.BOOST_STATE, Datapoint.CONTROL_MODE, Datapoint.SET_TEMPERATURE);

	protected static final List<Datapoint> LIST_THERMOMETER_HMIP = Arrays.asList(Datapoint.ACTUAL_TEMPERATURE,
			Datapoint.HUMIDITY);

	protected static final List<Datapoint> LIST_DIFFTHERMOMETER_HM = Arrays.asList(Datapoint.TEMPERATURE);

	protected static final List<Datapoint> LIST_SWITCH_HM = Arrays.asList(Datapoint.STATE);

	protected static final List<Datapoint> LIST_POWERMETER_HM = Arrays.asList(Datapoint.ENERGY_COUNTER,
			Datapoint.POWER);

	protected static final List<Datapoint> LIST_SYSVAR = Arrays.asList(Datapoint.SYSVAR_DUMMY);

	protected static final List<Datapoint> LIST_DOORBELL = Arrays.asList(Datapoint.PRESS_SHORT);

	protected static final List<Datapoint> LIST_CAMERA = Arrays.asList(Datapoint.STATE);

	private HomematicValueFormat homematicValueFormat;

	private Integer fixedChannel;

	private Datapoint(HomematicValueFormat homematicValueFormat, Integer fixedChannel) {
		this.homematicValueFormat = homematicValueFormat;
		this.fixedChannel = fixedChannel;
	}

	public String getHistorianPrefix() {
		return homematicValueFormat.getHistorianPrefix();
	}

	public HomematicValueFormat getValueFormat() {
		return homematicValueFormat;
	}

	public Integer getFixedChannel() {
		return fixedChannel;
	}
}
