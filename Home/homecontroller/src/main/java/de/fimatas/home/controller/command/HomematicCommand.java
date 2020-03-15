package de.fimatas.home.controller.command;

import static de.fimatas.home.controller.command.HomematicCommandConstants.EMPTY;

import org.springframework.util.Assert;

import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;

public class HomematicCommand {

	private CommandType commandType;

	private Device device;

	private Datapoint datapoint;

	private String suffix = EMPTY;

	private Boolean stateToSet;

	private String stringToSet;

	private String cashedVarName = null;

	protected HomematicCommand() {
		super();
	}

	public boolean isProgramRunCommand() {
		return commandType == CommandType.RUN_PROGRAM;
	}

	public Device getDevice() {
		return device;
	}

	public Datapoint getDatapoint() {
		return datapoint;
	}

	public String getSuffix() {
		return suffix;
	}

	public Boolean getStateToSet() {
		return stateToSet;
	}

	public String getStringToSet() {
		return stringToSet;
	}

	public String getCashedVarName() {
		return cashedVarName;
	}

	public CommandType getCommandType() {
		return commandType;
	}

	protected void setCommandType(CommandType commandType) {
		this.commandType = commandType;
	}

	protected void setDevice(Device device) {
		this.device = device;
	}

	protected void setDatapoint(Datapoint datapoint) {
		this.datapoint = datapoint;
	}

	protected void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	protected void setStateToSet(Boolean stateToSet) {
		this.stateToSet = stateToSet;
	}

	protected void setStringToSet(String stringToSet) {
		this.stringToSet = stringToSet;
	}

	protected void setCashedVarName(String cashedVarName) {
		this.cashedVarName = cashedVarName;
	}

	@Override
	public String toString() {
		Assert.notNull(cashedVarName, "toString(): cashedVarName is null!");
		return "HomematicCommand [buildVarName()=" + cashedVarName + "]";
	}

	@Override
	public int hashCode() {
		Assert.notNull(cashedVarName, "hashCode(): cashedVarName is null!");
		final int prime = 31;
		int result = 1;
		result = prime * result + cashedVarName.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		Assert.notNull(cashedVarName, "cashedVarName is null!");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HomematicCommand other = (HomematicCommand) obj;
		Assert.notNull(other.cashedVarName, "other.cashedVarName is null!");
		return cashedVarName.equals(cashedVarName);
	}

}
