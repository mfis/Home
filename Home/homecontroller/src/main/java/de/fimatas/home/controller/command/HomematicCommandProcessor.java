package de.fimatas.home.controller.command;

import static de.fimatas.home.controller.command.HomematicCommandConstants.BRACKET_CLOSE;
import static de.fimatas.home.controller.command.HomematicCommandConstants.BRACKET_OPEN;
import static de.fimatas.home.controller.command.HomematicCommandConstants.EMPTY;
import static de.fimatas.home.controller.command.HomematicCommandConstants.EQUAL;
import static de.fimatas.home.controller.command.HomematicCommandConstants.E_O_F;
import static de.fimatas.home.controller.command.HomematicCommandConstants.POINT;
import static de.fimatas.home.controller.command.HomematicCommandConstants.QUOTE;
import static de.fimatas.home.controller.command.HomematicCommandConstants.SEMICOLON;
import static de.fimatas.home.controller.command.HomematicCommandConstants.VAR;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.fimatas.home.controller.service.DeviceQualifier;
import de.fimatas.home.library.homematic.model.HomematicProtocol;
import de.fimatas.home.library.util.HomeUtils;

@Component
public class HomematicCommandProcessor {

    @Autowired
    private DeviceQualifier deviceQualifier;

    public String buildCommand(HomematicCommand command) {

        StringBuilder sb = new StringBuilder(140);

        String accessMethodParam = EMPTY;
        String dataFunctionParam = EMPTY;
        if (command.getCommandType().isHasDataFunctionParam()) {
            dataFunctionParam = varSetExpression(command);
        }

        sb.append(VAR);
        sb.append(buildVarName(command));
        sb.append(EQUAL);
        sb.append(command.getCommandType().getAccessMethod());

        switch (command.getCommandType()) {
        case GET_DEVICE_VALUE:
        case SET_DEVICE_STATE:
            if (command.getDevice().isSysVar()) {
                accessMethodParam = deviceQualifier.idFrom(command.getDevice());
            } else {
                accessMethodParam = datapointAdress(command);
            }
            break;
        case GET_DEVICE_VALUE_TS:
            accessMethodParam = datapointAdress(command);
            break;
        case GET_SYSVAR:
        case SET_SYSVAR:
            accessMethodParam = command.getSuffix();
            break;
        case GET_SYSVAR_DEVICEBASE:
        case SET_SYSVAR_DEVICEBASE:
        case RUN_PROGRAM:
            accessMethodParam = command.getDevice().programNamePrefix() + command.getSuffix();
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

        sb.append(command.getCommandType().getDataFunction());
        sb.append(BRACKET_OPEN);
        if (command.getCommandType().isHasDataFunctionParam()) {
            sb.append(dataFunctionParam);
        }
        sb.append(BRACKET_CLOSE);
        sb.append(SEMICOLON);

        return sb.toString();
    }

    public String buildVarName(HomematicCommand command) {

        if (command.getCashedVarName() != null) {
            return command.getCashedVarName();
        }

        StringBuilder sb = new StringBuilder(60);
        sb.append(command.getCommandType().getVarNamePrefix());
        String name;

        switch (command.getCommandType()) {
        case GET_DEVICE_VALUE:
        case SET_DEVICE_STATE:
        case GET_DEVICE_VALUE_TS:
            if (command.getDevice().isSysVar()) {
                name = deviceQualifier.idFrom(command.getDevice());
            } else {
                name = datapointAdress(command);
            }
            break;
        case GET_SYSVAR:
        case SET_SYSVAR:
            name = command.getSuffix();
            break;
        case GET_SYSVAR_DEVICEBASE:
        case SET_SYSVAR_DEVICEBASE:
        case RUN_PROGRAM:
            name = command.getDevice().programNamePrefix() + command.getSuffix();
            break;
        case EOF:
            name = E_O_F;
            break;
        default:
            throw new IllegalArgumentException("unknown CommandType");
        }

        sb.append(HomeUtils.escape(name));
        sb.append(command.getCommandType().getVarNameSuffix());
        command.setCashedVarName(sb.toString().toUpperCase());
        return command.getCashedVarName();
    }

    private String datapointAdress(HomematicCommand command) {

        // format: BidCos-RF.OEQ0854602:4.ACTUAL_TEMPERATURE"
        StringBuilder sb = new StringBuilder(63);
        sb.append(command.getDevice().getHomematicProtocol().getKey());
        sb.append("-");
        sb.append(HomematicProtocol.RF);
        sb.append(".");
        sb.append(deviceQualifier.idFrom(command.getDevice()));
        sb.append(":");
        sb.append(command.getDatapoint().getFixedChannel() == null ? deviceQualifier.channelFrom(command.getDevice()).toString()
            : command.getDatapoint().getFixedChannel().toString());
        sb.append(".");
        sb.append(command.getDatapoint().name());
        return sb.toString();
    }

    private String varSetExpression(HomematicCommand command) {

        if (command.getStringToSet() != null) {
            return QUOTE + command.getStringToSet() + QUOTE;
        }
        if (command.getStateToSet() != null) {
            return command.getStateToSet().toString();
        }
        if (command.getCommandType() == CommandType.EOF) {
            return QUOTE + E_O_F + QUOTE;
        }
        throw new IllegalArgumentException("no value to set");
    }

}
