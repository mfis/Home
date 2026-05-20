package de.fimatas.home.controller.command;

import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HomematicCommandBuilder {

    @Autowired
    private HomematicCommandProcessor homematicCommandProcessor;

    public HomematicCommand read(Device device, Datapoint datapoint) {
        HomematicCommand hc = new HomematicCommand();
        if (device.isSysVar()) {
            hc.setCommandType(HomematicCommandType.GET_SYSVAR_DEVICEBASE);
        } else {
            hc.setCommandType(HomematicCommandType.GET_DEVICE_VALUE);
        }
        hc.setDevice(device);
        hc.setDatapoint(datapoint);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand read(Device device, String suffix) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(HomematicCommandType.GET_SYSVAR_DEVICEBASE);
        hc.setDevice(device);
        hc.setSuffix(suffix);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand read(String name) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(HomematicCommandType.GET_SYSVAR);
        hc.setSuffix(name);
        if (name == null) {
            throw new IllegalArgumentException("null value to read!");
        }
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand readTS(Device device, Datapoint datapoint) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(HomematicCommandType.GET_DEVICE_VALUE_TS);
        hc.setDevice(device);
        hc.setDatapoint(datapoint);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(Device device, Datapoint datapoint, boolean stateToSet) {
        HomematicCommand hc = new HomematicCommand();
        if (device.isSysVar()) {
            hc.setCommandType(HomematicCommandType.GET_SYSVAR_DEVICEBASE);
        } else {
            hc.setCommandType(HomematicCommandType.SET_DEVICE_STATE);
        }
        hc.setDevice(device);
        hc.setDatapoint(datapoint);
        hc.setStateToSet(stateToSet);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(Device device, Datapoint datapoint, String stringToSet) {
        HomematicCommand hc = new HomematicCommand();
        if (device.isSysVar()) {
            hc.setCommandType(HomematicCommandType.SET_SYSVAR_DEVICEBASE);
        } else {
            hc.setCommandType(HomematicCommandType.SET_DEVICE_STATE);
        }
        hc.setDevice(device);
        hc.setDatapoint(datapoint);
        hc.setStringToSet(stringToSet);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(Device device, String suffix, boolean stateToSet) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(HomematicCommandType.SET_SYSVAR_DEVICEBASE);
        hc.setDevice(device);
        hc.setSuffix(suffix);
        hc.setStateToSet(stateToSet);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(Device device, String suffix, String stringToSet) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(HomematicCommandType.SET_SYSVAR_DEVICEBASE);
        hc.setDevice(device);
        hc.setSuffix(suffix);
        hc.setStringToSet(stringToSet);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand write(String name, String stringToSet) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(HomematicCommandType.SET_SYSVAR);
        hc.setSuffix(name);
        hc.setStringToSet(stringToSet);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand exec(Device device, String suffix) {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(HomematicCommandType.RUN_PROGRAM);
        hc.setDevice(device);
        hc.setSuffix(suffix);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    public HomematicCommand eof() {
        HomematicCommand hc = new HomematicCommand();
        hc.setCommandType(HomematicCommandType.EOF);
        hc.setVarName(cacheVarName(hc));
        return hc;
    }

    private String cacheVarName(HomematicCommand hc) {
        if (homematicCommandProcessor != null) {
            return homematicCommandProcessor.buildVarName(hc);
        }
        return null;
    }
}
