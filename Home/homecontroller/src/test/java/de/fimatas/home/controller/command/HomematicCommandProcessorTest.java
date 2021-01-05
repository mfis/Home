package de.fimatas.home.controller.command;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import de.fimatas.home.controller.service.DeviceQualifier;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;

@RunWith(MockitoJUnitRunner.class)
public class HomematicCommandProcessorTest {

    @InjectMocks
    private HomematicCommandProcessor processor;

    @InjectMocks
    private HomematicCommandBuilder homematicCommandBuilder;

    @Mock
    private DeviceQualifier deviceQualifier;

    @Before
    public void before() {
        when(deviceQualifier.idFrom(any(Device.class))).thenReturn("<ID>");
        when(deviceQualifier.channelFrom(any(Device.class))).thenReturn(99);
    }

    @Test
    public void testBuildCommandReadDeviceDatapoint() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.read(Device.STROMZAEHLER_GESAMT, Datapoint.POWER)),
            is("var VAR_BIDCOS_RF_<ID>_99_POWER = datapoints.Get('BidCos-RF.<ID>:99.POWER').Value();"));
    }

    @Test
    public void testBuildCommandReadDeviceDatapointSysVar() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.read(Device.AUSSENTEMPERATUR, Datapoint.SYSVAR_DUMMY)),
            is("var VAR_CONCLUSIONTEMPERATUREDRAUSSEN = dom.GetObject('ConclusionTemperatureDraussen').Value();"));
    }

    @Test
    public void testBuildCommandReadDeviceSuffix() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.read(Device.STROMZAEHLER_GESAMT, "SUFFIX")),
            is("var VAR_STROMHAUSSUFFIX = dom.GetObject('StromHausSUFFIX').Value();"));
    }

    @Test
    public void testBuildCommandReadName() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.read("NAME")),
            is("var VAR_NAME = dom.GetObject('NAME').Value();"));
    }

    @Test
    public void testBuildCommandReadTSDeviceDatapoint() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.readTS(Device.STROMZAEHLER_GESAMT, Datapoint.POWER)),
            is("var VAR_BIDCOS_RF_<ID>_99_POWER_TS = datapoints.Get('BidCos-RF.<ID>:99.POWER').LastTimestamp();"));
    }

    @Test
    public void testBuildCommandWriteDeviceDatapointBoolean() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.write(Device.STROMZAEHLER_GESAMT, Datapoint.POWER, true)),
            is("var RC_BIDCOS_RF_<ID>_99_POWER = datapoints.Get('BidCos-RF.<ID>:99.POWER').State(true);"));
    }

    @Test
    public void testBuildCommandWriteDeviceDatapointString() throws Exception {
        assertThat(
            processor.buildCommand(homematicCommandBuilder.write(Device.STROMZAEHLER_GESAMT, Datapoint.POWER, "NEWVALUE")),
            is("var RC_BIDCOS_RF_<ID>_99_POWER = datapoints.Get('BidCos-RF.<ID>:99.POWER').State('NEWVALUE');"));
    }

    @Test
    public void testBuildCommandWriteDeviceDatapointStringSysVar() throws Exception {
        assertThat(
            processor.buildCommand(homematicCommandBuilder.write(Device.AUSSENTEMPERATUR, Datapoint.SYSVAR_DUMMY, "NEWVALUE")),
            is("var RC_CONCLUSIONTEMPERATUREDRAUSSEN = dom.GetObject('ConclusionTemperatureDraussen').State('NEWVALUE');"));
    }

    @Test
    public void testBuildCommandWriteDeviceSuffixBoolean() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.write(Device.STROMZAEHLER_GESAMT, "SUFFIX", true)),
            is("var RC_STROMHAUSSUFFIX = dom.GetObject('StromHausSUFFIX').State(true);"));
    }

    @Test
    public void testBuildCommandWriteDeviceSuffixString() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.write(Device.STROMZAEHLER_GESAMT, "SUFFIX", "NEWVALUE")),
            is("var RC_STROMHAUSSUFFIX = dom.GetObject('StromHausSUFFIX').State('NEWVALUE');"));
    }

    @Test
    public void testBuildCommandWriteNameString() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.write("NAME", "NEWVALUE")),
            is("var RC_NAME = dom.GetObject('NAME').State('NEWVALUE');"));
    }

    @Test
    public void testBuildCommandExecDeviceSuffix() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.exec(Device.STROMZAEHLER_GESAMT, "SUFFIX")),
            is("var RC_STROMHAUSSUFFIX = dom.GetObject('StromHausSUFFIX').ProgramExecute();"));
    }

    @Test
    public void testBuildCommandEof() throws Exception {
        assertThat(processor.buildCommand(homematicCommandBuilder.eof()), is("var EOF = ('EOF');"));
    }
}
