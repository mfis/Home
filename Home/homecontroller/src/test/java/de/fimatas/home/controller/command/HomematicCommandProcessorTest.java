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
    public void testBuildCommandReadDeviceDatapoint() {
        assertThat(processor.buildCommand(homematicCommandBuilder.read(Device.STROMZAEHLER_BEZUG, Datapoint.IEC_POWER)),
            is("var VAR_BIDCOS_RF_<ID>_99_IEC_POWER = datapoints.Get('BidCos-RF.<ID>:99.IEC_POWER').Value();"));
    }

    @Test
    public void testBuildCommandReadDeviceDatapointSysVar() {
        assertThat(processor.buildCommand(homematicCommandBuilder.read(Device.AUSSENTEMPERATUR, Datapoint.SYSVAR_DUMMY)),
            is("var VAR_CONCLUSIONTEMPERATUREDRAUSSEN = dom.GetObject('ConclusionTemperatureDraussen').Value();"));
    }

    @Test
    public void testBuildCommandReadDeviceSuffix() {
        assertThat(processor.buildCommand(homematicCommandBuilder.read(Device.STROMZAEHLER_BEZUG, "SUFFIX")),
            is("var VAR_STROMBEZUGHAUSSUFFIX = dom.GetObject('StrombezugHausSUFFIX').Value();"));
    }

    @Test
    public void testBuildCommandReadName() {
        assertThat(processor.buildCommand(homematicCommandBuilder.read("NAME")),
            is("var VAR_NAME = dom.GetObject('NAME').Value();"));
    }

    @Test
    public void testBuildCommandReadTSDeviceDatapoint() {
        assertThat(processor.buildCommand(homematicCommandBuilder.readTS(Device.STROMZAEHLER_BEZUG, Datapoint.IEC_POWER)),
            is("var VAR_BIDCOS_RF_<ID>_99_IEC_POWER_TS = datapoints.Get('BidCos-RF.<ID>:99.IEC_POWER').Timestamp();"));
    }

    @Test
    public void testBuildCommandWriteDeviceDatapointBoolean() {
        assertThat(processor.buildCommand(homematicCommandBuilder.write(Device.STROMZAEHLER_BEZUG, Datapoint.IEC_POWER, true)),
            is("var RC_BIDCOS_RF_<ID>_99_IEC_POWER = datapoints.Get('BidCos-RF.<ID>:99.IEC_POWER').State(true);"));
    }

    @Test
    public void testBuildCommandWriteDeviceDatapointString() {
        assertThat(
            processor.buildCommand(homematicCommandBuilder.write(Device.STROMZAEHLER_BEZUG, Datapoint.IEC_POWER, "NEWVALUE")),
            is("var RC_BIDCOS_RF_<ID>_99_IEC_POWER = datapoints.Get('BidCos-RF.<ID>:99.IEC_POWER').State('NEWVALUE');"));
    }

    @Test
    public void testBuildCommandWriteDeviceDatapointStringSysVar() {
        assertThat(
            processor.buildCommand(homematicCommandBuilder.write(Device.AUSSENTEMPERATUR, Datapoint.SYSVAR_DUMMY, "NEWVALUE")),
            is("var RC_CONCLUSIONTEMPERATUREDRAUSSEN = dom.GetObject('ConclusionTemperatureDraussen').State('NEWVALUE');"));
    }

    @Test
    public void testBuildCommandWriteDeviceSuffixBoolean() {
        assertThat(processor.buildCommand(homematicCommandBuilder.write(Device.STROMZAEHLER_BEZUG, "SUFFIX", true)),
            is("var RC_STROMBEZUGHAUSSUFFIX = dom.GetObject('StrombezugHausSUFFIX').State(true);"));
    }

    @Test
    public void testBuildCommandWriteDeviceSuffixString() {
        assertThat(processor.buildCommand(homematicCommandBuilder.write(Device.STROMZAEHLER_BEZUG, "SUFFIX", "NEWVALUE")),
            is("var RC_STROMBEZUGHAUSSUFFIX = dom.GetObject('StrombezugHausSUFFIX').State('NEWVALUE');"));
    }

    @Test
    public void testBuildCommandWriteNameString() {
        assertThat(processor.buildCommand(homematicCommandBuilder.write("NAME", "NEWVALUE")),
            is("var RC_NAME = dom.GetObject('NAME').State('NEWVALUE');"));
    }

    @Test
    public void testBuildCommandExecDeviceSuffix() {
        assertThat(processor.buildCommand(homematicCommandBuilder.exec(Device.STROMZAEHLER_BEZUG, "SUFFIX")),
            is("var RC_STROMBEZUGHAUSSUFFIX = dom.GetObject('StrombezugHausSUFFIX').ProgramExecute();"));
    }

    @Test
    public void testBuildCommandEof() {
        assertThat(processor.buildCommand(homematicCommandBuilder.eof()), is("var EOF = ('EOF');"));
    }
}
