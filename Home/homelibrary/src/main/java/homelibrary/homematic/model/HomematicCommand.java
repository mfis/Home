package homelibrary.homematic.model;

import homecontroller.util.HomeUtils;

public class HomematicCommand {

	public static final String E_O_F = "EOF";

	public static final String PREFIX_VAR = "VAR_";

	public static final String PREFIX_RC = "RC_";

	private static final String SUFFIX_TS = "_TS";

	private static final String VAR = "var ";

	private static final String EQUAL = " = ";

	private static final String DOM_METHOD = "dom.GetObject";

	private static final String DATAPOINT_METHOD = "datapoints.Get";

	// read: last known value of the ccu.
	// write: only devices
	private static final String VALUE = "Value";

	// read: requests for new value.
	// write: devices and sysvar's
	private static final String STATE = "State";

	private static final String LAST_TIMESTAMP = "LastTimestamp";

	private static final String PROGRAM_EXECUTE = "ProgramExecute";

	private static final String POINT = ".";

	private static final String EMPTY = "";

	private static final String QUOTE = "'";

	private static final String BRACKET_OPEN = "(";

	private static final String BRACKET_CLOSE = ")";

	private static final String SEMICOLON = ";";

	private CommandType commandType;

	private Device device;

	private Datapoint datapoint;

	private String suffix = EMPTY;

	private Boolean stateToSet;

	private String stringToSet;

	private String cashedVarName = null;

	private HomematicCommand() {
		super();
	}

	public static HomematicCommand read(Device device, Datapoint datapoint) {
		HomematicCommand hc = new HomematicCommand();
		if (device.isSysVar()) {
			hc.commandType = CommandType.GET_SYSVAR_DEVICEBASE;
		} else {
			hc.commandType = CommandType.GET_DEVICE_VALUE;
		}
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
		if (device.isSysVar()) {
			hc.commandType = CommandType.GET_SYSVAR_DEVICEBASE;
		} else {
			hc.commandType = CommandType.SET_DEVICE_STATE;
		}
		hc.device = device;
		hc.datapoint = datapoint;
		hc.stateToSet = stateToSet;
		return hc;
	}

	public static HomematicCommand write(Device device, Datapoint datapoint, String stringToSet) {
		HomematicCommand hc = new HomematicCommand();
		if (device.isSysVar()) {
			hc.commandType = CommandType.SET_SYSVAR_DEVICEBASE;
		} else {
			hc.commandType = CommandType.SET_DEVICE_STATE;
		}
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

	public String buildCommand() {

		StringBuilder sb = new StringBuilder(140);

		String accessMethodParam = EMPTY;
		String dataFunctionParam = EMPTY;
		if (commandType.hasDataFunctionParam) {
			dataFunctionParam = varSetExpression();
		}

		sb.append(VAR);
		sb.append(buildVarName());
		sb.append(EQUAL);
		sb.append(commandType.accessMethod);

		switch (commandType) {
		case GET_DEVICE_VALUE:
		case SET_DEVICE_STATE:
			if (device.isSysVar()) {
				accessMethodParam = device.getId();
			} else {
				accessMethodParam = datapointAdress();
			}
			break;
		case GET_DEVICE_VALUE_TS:
			accessMethodParam = datapointAdress();
			break;
		case GET_SYSVAR:
		case SET_SYSVAR:
			accessMethodParam = suffix;
			break;
		case GET_SYSVAR_DEVICEBASE:
		case SET_SYSVAR_DEVICEBASE:
		case RUN_PROGRAM:
			accessMethodParam = device.programNamePrefix() + suffix;
			break;
		case EOF:
			break;
		default:
			throw new IllegalArgumentException("unknown CommandType");
		}

		if (!EMPTY.equals(accessMethodParam)) {
			sb.append(BRACKET_OPEN);
			sb.append(QUOTE);
			sb.append(accessMethodParam);
			sb.append(QUOTE);
			sb.append(BRACKET_CLOSE);
			sb.append(POINT);
		}

		sb.append(commandType.dataFunction);
		sb.append(BRACKET_OPEN);
		if (commandType.hasDataFunctionParam) {
			sb.append(dataFunctionParam);
		}
		sb.append(BRACKET_CLOSE);
		sb.append(SEMICOLON);

		return sb.toString();
	}

	public String buildVarName() {

		if (cashedVarName != null) {
			return cashedVarName;
		}

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
		cashedVarName = sb.toString().toUpperCase();
		return cashedVarName;
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
			return QUOTE + stringToSet + QUOTE;
		}
		if (stateToSet != null) {
			return stateToSet.toString();
		}
		if (commandType == CommandType.EOF) {
			return QUOTE + E_O_F + QUOTE;
		}
		throw new IllegalArgumentException("no value to set");
	}

	private enum CommandType {

		GET_DEVICE_VALUE(PREFIX_VAR, EMPTY, DATAPOINT_METHOD, VALUE, false), //
		GET_DEVICE_VALUE_TS(PREFIX_VAR, SUFFIX_TS, DATAPOINT_METHOD, LAST_TIMESTAMP, false), //
		GET_SYSVAR(PREFIX_VAR, EMPTY, DOM_METHOD, VALUE, false), //
		GET_SYSVAR_DEVICEBASE(PREFIX_VAR, EMPTY, DOM_METHOD, VALUE, false), //
		SET_SYSVAR(PREFIX_RC, EMPTY, DOM_METHOD, STATE, true), //
		SET_SYSVAR_DEVICEBASE(PREFIX_RC, EMPTY, DOM_METHOD, STATE, true), //
		SET_DEVICE_STATE(PREFIX_RC, EMPTY, DATAPOINT_METHOD, STATE, true), //
		RUN_PROGRAM(PREFIX_RC, EMPTY, DOM_METHOD, PROGRAM_EXECUTE, false), //
		EOF(EMPTY, EMPTY, EMPTY, EMPTY, true), //
		;

		private String varNamePrefix;

		private String varNameSuffix;

		private String accessMethod;

		private String dataFunction;

		private boolean hasDataFunctionParam;

		private CommandType(String varNamePrefix, String varNameSuffix, String accessMethod,
				String dataFunction, boolean hasDataFunctionParam) {
			this.varNamePrefix = varNamePrefix;
			this.varNameSuffix = varNameSuffix;
			this.accessMethod = accessMethod;
			this.dataFunction = dataFunction;
			this.hasDataFunctionParam = hasDataFunctionParam;
		}
	}

	public boolean isProgramRunCommand() {
		return commandType == CommandType.RUN_PROGRAM;
	}

	@Override
	public String toString() {
		return "HomematicCommand [buildVarName()=" + this.buildVarName() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + buildVarName().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HomematicCommand other = (HomematicCommand) obj;
		return this.buildVarName().equals(other.buildVarName());
	}

}
