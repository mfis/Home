package homelibrary.homematic.model;

import org.springframework.util.StringUtils;

public class HomematicCommand {

	public static final String E_O_F = "EOF";

	public static final String PREFIX_VAR = "VAR_";

	public static final String PREFIX_RC = "RC_";

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

	public static HomematicCommand write(Device device, String stringToSet) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.SET_SYSVAR;
		hc.device = device;
		hc.stringToSet = stringToSet;
		return hc;
	}

	public static HomematicCommand exec(Device device, String suffix) {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.SET_SYSVAR_DEVICEBASE;
		hc.device = device;
		hc.suffix = suffix;
		return hc;
	}

	public static HomematicCommand eof() {
		HomematicCommand hc = new HomematicCommand();
		hc.commandType = CommandType.EOF;
		return hc;
	}

	// TODO: write unit tests!
	public String buildCommand() { // TODO: Refactoring needed!

		// var v1 =
		// (datapoints.Get('BidCos-RF.OEQ0854602:4.ACTUAL_TEMPERATURE')).Value();
		// var v1a =
		// (datapoints.Get('BidCos-RF.OEQ0854602:4.ACTUAL_TEMPERATURE')).State();
		// var v2 =
		// (datapoints.Get('HmIP-RF.000E9A498BA811:1.ACTUAL_TEMPERATURE')).Value();
		// var v3 = dom.GetObject('TestProgramm').ProgramExecute();
		// var s1 = dom.GetObject('testvar').State('abc');
		// var v4 = dom.GetObject('testvar').State();
		// var s2 = dom.GetObject('testbool').State(false);
		// var v5 = dom.GetObject('testbool').State();
		// var v6 =
		// (datapoints.Get('BidCos-RF.OEQ0712456:1.STATE')).State(true);

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

	public String buildVarName() { // TODO: Refactoring needed!

		switch (commandType) {
		case GET_DEVICE_VALUE:
			if (device.isSysVar()) {
				return PREFIX_VAR + escape(device.getId());
			} else {
				return PREFIX_VAR + escape(datapointAdress());
			}
		case GET_DEVICE_VALUE_TS:
			return PREFIX_VAR + escape(datapointAdress() + "_TS");
		case SET_DEVICE_STATE:
			if (device.isSysVar()) {
				return PREFIX_RC + escape(device.getId());
			} else {
				return PREFIX_RC + escape(datapointAdress());
			}
		case GET_SYSVAR:
			return PREFIX_VAR + escape(suffix);
		case SET_SYSVAR:
			return PREFIX_RC + escape(suffix);
		case GET_SYSVAR_DEVICEBASE:
			return PREFIX_VAR + escape(device.programNamePrefix() + suffix);
		case SET_SYSVAR_DEVICEBASE:
		case RUN_PROGRAM:
			return PREFIX_RC + escape(device.programNamePrefix() + suffix);
		case EOF:
			return E_O_F;
		default:
			throw new IllegalArgumentException("unknown CommandType");
		}
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

	protected static String escape(String string) { // TODO: Refactoring needed!
		string = StringUtils.replace(string, " ", "");
		string = StringUtils.replace(string, ".", "_");
		string = StringUtils.replace(string, "-", "_");
		string = StringUtils.replace(string, ":", "_");
		string = StringUtils.replace(string, "ä", "ae");
		string = StringUtils.replace(string, "ö", "oe");
		string = StringUtils.replace(string, "ü", "ue");
		string = StringUtils.replace(string, "Ä", "Ae");
		string = StringUtils.replace(string, "Ö", "Oe");
		string = StringUtils.replace(string, "Ü", "Ue");
		string = StringUtils.replace(string, "ß", "ss");
		return string;
	}

	private enum CommandType {
		GET_DEVICE_VALUE, GET_DEVICE_VALUE_TS, GET_SYSVAR, GET_SYSVAR_DEVICEBASE, SET_SYSVAR, SET_SYSVAR_DEVICEBASE, SET_DEVICE_STATE, RUN_PROGRAM, EOF;
	}

	@Override
	public String toString() {
		return "HomematicCommand [commandType=" + commandType + ", device=" + device + ", datapoint="
				+ datapoint + ", suffix=" + suffix + ", stateToSet=" + stateToSet + ", stringToSet="
				+ stringToSet + "]";
	}

}
