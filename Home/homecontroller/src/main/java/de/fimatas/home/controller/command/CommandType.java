package de.fimatas.home.controller.command;

import static de.fimatas.home.controller.command.HomematicCommandConstants.DATAPOINT_METHOD;
import static de.fimatas.home.controller.command.HomematicCommandConstants.DOM_METHOD;
import static de.fimatas.home.controller.command.HomematicCommandConstants.EMPTY;
import static de.fimatas.home.controller.command.HomematicCommandConstants.LAST_TIMESTAMP;
import static de.fimatas.home.controller.command.HomematicCommandConstants.PREFIX_RC;
import static de.fimatas.home.controller.command.HomematicCommandConstants.PREFIX_VAR;
import static de.fimatas.home.controller.command.HomematicCommandConstants.PROGRAM_EXECUTE;
import static de.fimatas.home.controller.command.HomematicCommandConstants.STATE;
import static de.fimatas.home.controller.command.HomematicCommandConstants.SUFFIX_TS;
import static de.fimatas.home.controller.command.HomematicCommandConstants.VALUE;

public enum CommandType {

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

    private final String varNamePrefix;

    private final String varNameSuffix;

    private final String accessMethod;

    private final String dataFunction;

    private final boolean hasDataFunctionParam;

    private CommandType(String varNamePrefix, String varNameSuffix, String accessMethod, String dataFunction,
            boolean hasDataFunctionParam) {
        this.varNamePrefix = varNamePrefix;
        this.varNameSuffix = varNameSuffix;
        this.accessMethod = accessMethod;
        this.dataFunction = dataFunction;
        this.hasDataFunctionParam = hasDataFunctionParam;
    }

    public String getVarNamePrefix() {
        return varNamePrefix;
    }

    public String getVarNameSuffix() {
        return varNameSuffix;
    }

    public String getAccessMethod() {
        return accessMethod;
    }

    public String getDataFunction() {
        return dataFunction;
    }

    public boolean isHasDataFunctionParam() {
        return hasDataFunctionParam;
    }

}
