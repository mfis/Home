package de.fimatas.home.controller.command;

public class HomematicCommandConstants {

    private HomematicCommandConstants() {
        super();
    }

    public static final String E_O_F = "EOF";

    public static final String PREFIX_VAR = "VAR_";

    public static final String PREFIX_RC = "RC_";

    public static final String SUFFIX_TS = "_TS";

    static final String EMPTY = "";

    static final String DOM_METHOD = "dom.GetObject";

    static final String DATAPOINT_METHOD = "datapoints.Get";

    // read: last known value of the ccu.
    // write: only devices
    static final String VALUE = "Value";

    // read: requests for new value.
    // write: devices and sysvar's
    static final String STATE = "State";

    static final String SEND_TIMESTAMP = "Timestamp";

    static final String PROGRAM_EXECUTE = "ProgramExecute";

    static final String VAR = "var ";

    static final String EQUAL = " = ";

    static final String POINT = ".";

    static final String QUOTE = "'";

    static final String BRACKET_OPEN = "(";

    static final String BRACKET_CLOSE = ")";

    static final String SEMICOLON = ";";
}
