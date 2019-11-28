package homelibrary.homematic.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HomematicCommandTest {

	@Test
	public void testBuildCommandReadDeviceDatapoint() throws Exception {
		assertThat(HomematicCommand.read(Device.STROMZAEHLER, Datapoint.POWER).buildCommand(), is(
				"var VAR_BidCos_RF_NEQ0861520_1_POWER = datapoints.Get('BidCos-RF.NEQ0861520:1.POWER').Value();"));
	}

	@Test
	public void testBuildCommandReadDeviceDatapointSysVar() throws Exception {
		assertThat(HomematicCommand.read(Device.AUSSENTEMPERATUR, Datapoint.SYSVAR_DUMMY).buildCommand(),
				is(""));
	}

	@Test
	public void testBuildCommandReadDeviceSuffix() throws Exception {
		assertThat(HomematicCommand.read(Device.STROMZAEHLER, "SUFFIX").buildCommand(),
				is("var VAR_StromverbrauchHausSUFFIX = dom.GetObject('StromverbrauchHausSUFFIX').Value();"));
	}

	@Test
	public void testBuildCommandReadName() throws Exception {
		assertThat(HomematicCommand.read("NAME").buildCommand(),
				is("var VAR_NAME = dom.GetObject('NAME').Value();"));
	}

	@Test
	public void testBuildCommandReadTSDeviceDatapoint() throws Exception {
		assertThat(HomematicCommand.readTS(Device.STROMZAEHLER, Datapoint.POWER).buildCommand(), is(
				"var VAR_BidCos_RF_NEQ0861520_1_POWER_TS = datapoints.Get('BidCos-RF.NEQ0861520:1.POWER').LastTimestamp();"));
	}

	@Test
	public void testBuildCommandWriteDeviceDatapointBoolean() throws Exception {
		assertThat(HomematicCommand.write(Device.STROMZAEHLER, Datapoint.POWER, true).buildCommand(), is(
				"var RC_BidCos_RF_NEQ0861520_1_POWER = datapoints.Get('BidCos-RF.NEQ0861520:1.POWER').State(true);"));
	}

	@Test
	public void testBuildCommandWriteDeviceDatapointString() throws Exception {
		assertThat(HomematicCommand.write(Device.STROMZAEHLER, Datapoint.POWER, "NEWVALUE").buildCommand(),
				is("var RC_BidCos_RF_NEQ0861520_1_POWER = datapoints.Get('BidCos-RF.NEQ0861520:1.POWER').State('NEWVALUE');"));
	}

	@Test
	public void testBuildCommandWriteDeviceDatapointStringSysVar() throws Exception {
		assertThat(HomematicCommand.write(Device.AUSSENTEMPERATUR, Datapoint.SYSVAR_DUMMY, "NEWVALUE")
				.buildCommand(), is(""));
	}

	@Test
	public void testBuildCommandWriteDeviceSuffixBoolean() throws Exception {
		assertThat(HomematicCommand.write(Device.STROMZAEHLER, "SUFFIX", true).buildCommand(), is(
				"var RC_StromverbrauchHausSUFFIX = dom.GetObject('StromverbrauchHausSUFFIX').State(true);"));
	}

	@Test
	public void testBuildCommandWriteDeviceSuffixString() throws Exception {
		assertThat(HomematicCommand.write(Device.STROMZAEHLER, "SUFFIX", "NEWVALUE").buildCommand(), is(
				"var RC_StromverbrauchHausSUFFIX = dom.GetObject('StromverbrauchHausSUFFIX').State('NEWVALUE');"));
	}

	@Test
	public void testBuildCommandWriteNameString() throws Exception {
		assertThat(HomematicCommand.write("NAME", "NEWVALUE").buildCommand(),
				is("var RC_NAME = dom.GetObject('NAME').State('NEWVALUE');"));
	}

	@Test
	public void testBuildCommandExecDeviceSuffix() throws Exception {
		assertThat(HomematicCommand.exec(Device.STROMZAEHLER, "SUFFIX").buildCommand(), is(
				"var RC_StromverbrauchHausSUFFIX = dom.GetObject('StromverbrauchHausSUFFIX').ProgramExecute();"));
	}

	@Test
	public void testBuildCommandEof() throws Exception {
		assertThat(HomematicCommand.eof().buildCommand(), is("var EOF = ('EOF');"));
	}
}
