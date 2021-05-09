package de.fimatas.home.controller.model;

import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.library.homematic.model.HistoryStrategy;

public class HistoryElement {

    private final HomematicCommand command;

    private final HistoryStrategy strategy;

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
}
