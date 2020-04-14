package de.fimatas.home.library.homematic.model;

import java.util.Arrays;
import java.util.List;

public enum Datapoint {

	TEMPERATURE(HomematicValueFormat.DEC, null, false), //
	ACTUAL_TEMPERATURE(HomematicValueFormat.DEC, null, false), //
	HUMIDITY(HomematicValueFormat.DEC, null, false), //
	CONTROL_MODE(HomematicValueFormat.DEC, null, false), //
	BOOST_STATE(HomematicValueFormat.DEC, null, false), //
	SET_TEMPERATURE(HomematicValueFormat.DEC, null, false), //
	STATE(HomematicValueFormat.DEC, null, false), //
	STATE_UNCERTAIN(HomematicValueFormat.DEC, null, false), //
	POWER(HomematicValueFormat.DEC, null, false), //
	LOWBAT(HomematicValueFormat.DEC, 0, false), //
	LOW_BAT(HomematicValueFormat.DEC, 0, false), //
	ENERGY_COUNTER(HomematicValueFormat.DEC, null, false), //
	VALUE(HomematicValueFormat.DEC, null, false), //
	PRESS_SHORT(HomematicValueFormat.DEC, null, true), //
	ERROR(HomematicValueFormat.DEC, null, false), //
	SYSVAR_DUMMY(null, null, false), //
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
	
	protected static final List<Datapoint> LIST_DOORLOCK = Arrays.asList(Datapoint.STATE, Datapoint.STATE_UNCERTAIN, Datapoint.ERROR);

	private HomematicValueFormat homematicValueFormat;

	private Integer fixedChannel;

	private boolean timestamp;

	private Datapoint(HomematicValueFormat homematicValueFormat, Integer fixedChannel, boolean timestamp) {
		this.homematicValueFormat = homematicValueFormat;
		this.fixedChannel = fixedChannel;
		this.timestamp = timestamp;
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

	public boolean isTimestamp() {
		return timestamp;
	}
}
