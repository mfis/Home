package de.fimatas.home.library.homematic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Datapoint {

    TEMPERATURE(HomematicValueFormat.DEC, null, true), //
    ACTUAL_TEMPERATURE(HomematicValueFormat.DEC, null, true), //
    HUMIDITY(HomematicValueFormat.DEC, null, false), //
    CONTROL_MODE(HomematicValueFormat.DEC, null, true), //
    SET_POINT_MODE(HomematicValueFormat.DEC, null, true), //
    BOOST_STATE(HomematicValueFormat.DEC, null, false), //
    BOOST_TIME(HomematicValueFormat.DEC, null, false), //
    BOOST_MODE(HomematicValueFormat.DEC, null, false), //
    SET_TEMPERATURE(HomematicValueFormat.DEC, null, false), //
    SET_POINT_TEMPERATURE(HomematicValueFormat.DEC, null, false), //
    STATE(HomematicValueFormat.DEC, null, true), //
    STATE_UNCERTAIN(HomematicValueFormat.DEC, null, false), //
    POWER(HomematicValueFormat.DEC, null, true), //
    IEC_POWER(HomematicValueFormat.DEC, null, true), //
    LOWBAT(HomematicValueFormat.DEC, 0, false), //
    LOW_BAT(HomematicValueFormat.DEC, 0, false), //
    ENERGY_COUNTER(HomematicValueFormat.DEC, null, false), //
    IEC_ENERGY_COUNTER(HomematicValueFormat.DEC, null, false), //
    VALUE(HomematicValueFormat.DEC, null, true), //
    PRESS_SHORT(HomematicValueFormat.DEC, null, true), //
    ERROR(HomematicValueFormat.DEC, null, false), //
    UNREACH(HomematicValueFormat.DEC, 0, false), //
    SYSVAR_DUMMY(null, null, false), //
    GAS_ENERGY_COUNTER(HomematicValueFormat.DEC, null, true), //
    GAS_POWER(HomematicValueFormat.DEC, null, false), //
    ;

    protected static final List<Datapoint> LIST_THERMOSTAT_HM =
        Arrays.asList(Datapoint.ACTUAL_TEMPERATURE, Datapoint.BOOST_STATE, Datapoint.CONTROL_MODE, Datapoint.SET_TEMPERATURE);

    protected static final List<Datapoint> LIST_THERMOSTAT_HMIP =
            Arrays.asList(Datapoint.ACTUAL_TEMPERATURE, Datapoint.BOOST_TIME, Datapoint.SET_POINT_MODE, Datapoint.SET_POINT_TEMPERATURE, Datapoint.BOOST_MODE);

    protected static final List<Datapoint> LIST_THERMOMETER_HMIP =
        Arrays.asList(Datapoint.ACTUAL_TEMPERATURE, Datapoint.HUMIDITY);

    protected static final List<Datapoint> LIST_DIFFTHERMOMETER_HM = Arrays.asList(Datapoint.TEMPERATURE);

    protected static final List<Datapoint> LIST_SWITCH_HM = Arrays.asList(Datapoint.STATE);

    protected static final List<Datapoint> LIST_POWERMETER_HM = Arrays.asList(Datapoint.ENERGY_COUNTER, Datapoint.POWER);

    protected static final List<Datapoint> LIST_POWERMETER_IEC = Arrays.asList(Datapoint.IEC_ENERGY_COUNTER, Datapoint.IEC_POWER);

    protected static final List<Datapoint> LIST_GASMETER_HM = Arrays.asList(Datapoint.GAS_ENERGY_COUNTER, Datapoint.GAS_POWER);

    protected static final List<Datapoint> LIST_SYSVAR = Arrays.asList(Datapoint.SYSVAR_DUMMY);

    protected static final List<Datapoint> LIST_DOORBELL = Arrays.asList(Datapoint.PRESS_SHORT);

    protected static final List<Datapoint> LIST_WINDOW_SENSOR = Arrays.asList(Datapoint.STATE);

    protected static final List<Datapoint> LIST_EMPTY = new ArrayList<>();

    protected static final List<Datapoint> LIST_DOORLOCK =
        Arrays.asList(Datapoint.STATE, Datapoint.STATE_UNCERTAIN, Datapoint.ERROR);

    private HomematicValueFormat homematicValueFormat;

    private Integer fixedChannel;

    private boolean readTimestamp;

    private Datapoint(HomematicValueFormat homematicValueFormat, Integer fixedChannel, boolean readTimestamp) {
        this.homematicValueFormat = homematicValueFormat;
        this.fixedChannel = fixedChannel;
        this.readTimestamp = readTimestamp;
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

    public boolean isReadTimestamp() {
        return readTimestamp;
    }

}
