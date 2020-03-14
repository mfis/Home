package homecontroller.model;

import homecontroller.command.HomematicCommand;
import homelibrary.homematic.model.HistoryStrategy;

public class HistoryElement {

	private HomematicCommand command;

	private HistoryStrategy strategy;

	private int valueDifferenceToSave;

	public HistoryElement(HomematicCommand command, HistoryStrategy strategy, int valueDifferenceToSave) {
		this.command = command;
		this.strategy = strategy;
		this.valueDifferenceToSave = valueDifferenceToSave;
	}

	public HomematicCommand getCommand() {
		return command;
	}

	public HistoryStrategy getStrategy() {
		return strategy;
	}

	public int getValueDifferenceToSave() {
		return valueDifferenceToSave;
	}

	void setCommand(HomematicCommand command) {
		this.command = command;
	}

	void setStrategy(HistoryStrategy strategy) {
		this.strategy = strategy;
	}

	void setValueDifferenceToSave(int valueDifferenceToSave) {
		this.valueDifferenceToSave = valueDifferenceToSave;
	}
	
}
