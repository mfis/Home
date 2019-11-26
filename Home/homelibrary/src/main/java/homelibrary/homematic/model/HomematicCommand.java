package homelibrary.homematic.model;

import homecontroller.util.HomeUtils;

public class HomematicCommand {

	public static final String E_O_F = "EOF";

	public static final String PREFIX_VAR = "VAR_";

	public static final String PREFIX_RC = "RC_";

	private static final String SUFFIX_TS = "_TS";

	private static final String EMPTY = "";

	private CommandType commandType;

	private Device device;

	private Datapoint datapoint;

	private String suffix;

	private Boolean stateToSet;

	private String stringToSet;

	private HomematicCommand() {
		super();
	}

	public static HomematicCommand read(Device device, Datapoint datapoint) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.GET_DEVICE_VALUE;
		hc.device = device;
		hc.datapoint = datapoint;
		return hc;
	}

	public static HomematicCommand read(Device device, String suffix) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.GET_SYSVAR_DEVICEBASE;
		hc.device = device;
		hc.suffix = suffix;
		return hc;
	}

	public static HomematicCommand read(String name) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.GET_SYSVAR;
		hc.suffix = name;
		if (name == null) {
			throw new IllegalArgumentException("null value to read!");
		}
		return hc;
	}

	public static HomematicCommand readTS(Device device, Datapoint datapoint) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.GET_DEVICE_VALUE_TS;
		hc.device = device;
		hc.datapoint = datapoint;
		return hc;
	}

	public static HomematicCommand write(Device device, Datapoint datapoint, boolean stateToSet) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.SET_DEVICE_STATE;
		hc.device = device;
		hc.datapoint = datapoint;
		hc.stateToSet = stateToSet;
		return hc;
	}

	public static HomematicCommand write(Device device, Datapoint datapoint, String stringToSet) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.SET_DEVICE_STATE;
		hc.device = device;
		hc.datapoint = datapoint;
		hc.stringToSet = stringToSet;
		return hc;
	}

	public static HomematicCommand write(Device device, String suffix, boolean stateToSet) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.SET_SYSVAR_DEVICEBASE;
		hc.device = device;
		hc.suffix = suffix;
		hc.stateToSet = stateToSet;
		return hc;
	}

	public static HomematicCommand write(Device device, String suffix, String stringToSet) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.SET_SYSVAR_DEVICEBASE;
		hc.device = device;
		hc.suffix = suffix;
		hc.stringToSet = stringToSet;
		return hc;
	}

	public static HomematicCommand write(String name, String stringToSet) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.SET_SYSVAR;
		hc.suffix = name;
		hc.stringToSet = stringToSet;
		return hc;
	}

	public static HomematicCommand exec(Device device, String suffix) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.RUN_PROGRAM;
		hc.device = device;
		hc.suffix = suffix;
		return hc;
	}

	public static HomematicCommand eof() {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.EOF;
		return hc;
	}

	public String buildCommand() { // TODO: Refactoring needed!

		switch (commandType) {
		case GET_DEVICE_VALUE:
			if (device.isSysVar()) {
				return "var " + buildVarName() + " = dom.GetObject('" + device.getId() + "').State();";
			} else {
				return "var " + buildVarName() + " = (datapoints.Get('" + datapointAdress() + "')).Value();";
			}
		case GET_DEVICE_VALUE_TS:
			return "var " + buildVarName() + " = (datapoints.Get('" + datapointAdress()
					+ "')).LastTimestamp();";
		case GET_SYSVAR:
			return "var " + buildVarName() + " = dom.GetObject('" + suffix + "').State();";
		case GET_SYSVAR_DEVICEBASE:
			return "var " + buildVarName() + " = dom.GetObject('" + device.programNamePrefix() + suffix
					+ "').State();";
		case SET_DEVICE_STATE:
			if (device.isSysVar()) {
				return "var " + buildVarName() + " = dom.GetObject('" + device.getId() + "').State("
						+ varSetExpression() + ");";
			} else {
				return "var " + buildVarName() + " = (datapoints.Get('" + datapointAdress() + "')).State("
						+ varSetExpression() + ");";
			}
		case SET_SYSVAR_DEVICEBASE:
			return "var " + buildVarName() + " = dom.GetObject('" + device.programNamePrefix() + suffix
					+ "').State(" + varSetExpression() + ");";
		case SET_SYSVAR:
			return "var " + buildVarName() + " = dom.GetObject('" + suffix + "').State(" + varSetExpression()
					+ ");";
		case RUN_PROGRAM:
			return "var " + buildVarName() + " = dom.GetObject('" + device.programNamePrefix() + suffix
					+ "').ProgramExecute();";
		case EOF:
			return "var " + buildVarName() + " = '" + E_O_F + "';";
		default:
			throw new IllegalArgumentException("unknown CommandType");
		}
	}

	public String buildVarName() {

		StringBuilder sb = new StringBuilder(60);
		sb.append(commandType.varNamePrefix);
		String name;

		switch (commandType) {
		case GET_DEVICE_VALUE:
		case SET_DEVICE_STATE:
		case GET_DEVICE_VALUE_TS:
			if (device.isSysVar()) {
				name = device.getId();
			} else {
				name = datapointAdress();
			}
			break;
		case GET_SYSVAR:
		case SET_SYSVAR:
			name = suffix;
			break;
		case GET_SYSVAR_DEVICEBASE:
		case SET_SYSVAR_DEVICEBASE:
		case RUN_PROGRAM:
			name = device.programNamePrefix() + suffix;
			break;
		case EOF:
			name = E_O_F;
			break;
		default:
			throw new IllegalArgumentException("unknown CommandType");
		}

		sb.append(HomeUtils.escape(name));
		sb.append(commandType.varNameSuffix);
		return sb.toString();
	}

	private String datapointAdress() {

		// format: BidCos-RF.OEQ0854602:4.ACTUAL_TEMPERATURE"
		StringBuilder sb = new StringBuilder(63);
		sb.append(device.getHomematicProtocol().getKey());
		sb.append("-");
		sb.append(HomematicProtocol.RF);
		sb.append(".");
		sb.append(device.getId());
		sb.append(":");
		sb.append(datapoint.getFixedChannel() == null ? device.getChannel().toString()
				: datapoint.getFixedChannel().toString());
		sb.append(".");
		sb.append(datapoint.name());
		return sb.toString();
	}

	private String varSetExpression() {
		if (stringToSet != null) {
			return "'" + stringToSet + "'";
		}
		if (stateToSet != null) {
			return stateToSet.toString();
		}
		throw new IllegalArgumentException("no value to set");
	}

	private enum CommandType {

		GET_DEVICE_VALUE(PREFIX_VAR, EMPTY), //
		GET_DEVICE_VALUE_TS(PREFIX_VAR, SUFFIX_TS), //
		GET_SYSVAR(PREFIX_VAR, EMPTY), //
		GET_SYSVAR_DEVICEBASE(PREFIX_VAR, EMPTY), //
		SET_SYSVAR(PREFIX_RC, EMPTY), //
		SET_SYSVAR_DEVICEBASE(PREFIX_RC, EMPTY), //
		SET_DEVICE_STATE(PREFIX_RC, EMPTY), //
		RUN_PROGRAM(PREFIX_RC, EMPTY), //
		EOF(EMPTY, EMPTY), //
		;

		private String varNamePrefix;

		private String varNameSuffix;

		private CommandType(String varNamePrefix, String varNameSuffix) {
			this.varNamePrefix = varNamePrefix;
			this.varNameSuffix = varNameSuffix;
		}
	}

	public boolean isProgramRunCommand() {
		return commandType == CommandType.RUN_PROGRAM;
	}

	@Override
	public String toString() {
		return "HomematicCommand [commandType=" + commandType + ", device=" + device + ", datapoint="
				+ datapoint + ", suffix=" + suffix + ", stateToSet=" + stateToSet + ", stringToSet="
				+ stringToSet + "]";
	}

}
