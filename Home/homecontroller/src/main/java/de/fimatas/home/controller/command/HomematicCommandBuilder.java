package de.fimatas.home.controller.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;

@Component
public class HomematicCommandBuilder {

    @Autowired
    private HomematicCommandProcessor homematicCommandProcessor;

    public HomematicCommand read(Device device, Datapoint datapoint) {
        HomematicCommand hc = new HomematicCommand();
        if (device.isSysVar()) {
            hc.setCommandType(CommandType.GET_SYSVAR_DEVICEBASE);
        } else {
            hc.setCommandType(CommandType.GET_DEVICE_VALUE);
        }
        hc.setDevice(device);
        hc.setDatapoint(datapoint);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand read(Device device, String suffix) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(CommandType.GET_SYSVAR_DEVICEBASE);
        hc.setDevice(device);
        hc.setSuffix(suffix);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand read(String name) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(CommandType.GET_SYSVAR);
        hc.setSuffix(name);
        if (name == null) {
            throw new IllegalArgumentException("null value to read!");
        }
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand readTS(Device device, Datapoint datapoint) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(CommandType.GET_DEVICE_VALUE_TS);
        hc.setDevice(device);
        hc.setDatapoint(datapoint);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(Device device, Datapoint datapoint, boolean stateToSet) {
        HomematicCommand hc = new HomematicCommand();
        if (device.isSysVar()) {
            hc.setCommandType(CommandType.GET_SYSVAR_DEVICEBASE);
        } else {
            hc.setCommandType(CommandType.SET_DEVICE_STATE);
        }
        hc.setDevice(device);
        hc.setDatapoint(datapoint);
        hc.setStateToSet(stateToSet);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(Device device, Datapoint datapoint, String stringToSet) {
        HomematicCommand hc = new HomematicCommand();
        if (device.isSysVar()) {
            hc.setCommandType(CommandType.SET_SYSVAR_DEVICEBASE);
        } else {
            hc.setCommandType(CommandType.SET_DEVICE_STATE);
        }
        hc.setDevice(device);
        hc.setDatapoint(datapoint);
        hc.setStringToSet(stringToSet);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(Device device, String suffix, boolean stateToSet) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(CommandType.SET_SYSVAR_DEVICEBASE);
        hc.setDevice(device);
        hc.setSuffix(suffix);
        hc.setStateToSet(stateToSet);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(Device device, String suffix, String stringToSet) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(CommandType.SET_SYSVAR_DEVICEBASE);
        hc.setDevice(device);
        hc.setSuffix(suffix);
        hc.setStringToSet(stringToSet);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(String name, String stringToSet) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(CommandType.SET_SYSVAR);
        hc.setSuffix(name);
        hc.setStringToSet(stringToSet);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand exec(Device device, String suffix) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(CommandType.RUN_PROGRAM);
        hc.setDevice(device);
        hc.setSuffix(suffix);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand eof() {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(CommandType.EOF);
        hc.setCashedVarName(cacheVarName(hc));
        return hc;
    }

    private String cacheVarName(HomematicCommand hc) {
        if (homematicCommandProcessor != null) {
            return homematicCommandProcessor.buildVarName(hc);
        }
        return null;
    }
}
