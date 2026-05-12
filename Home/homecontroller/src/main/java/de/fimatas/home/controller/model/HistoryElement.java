package de.fimatas.home.controller.model;

import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.library.homematic.model.HistoryStrategy;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class HistoryElement {

    private HomematicCommand command;

    private HistoryStrategy strategy;

    private BigDecimal valueDifferenceToSave;

    public HistoryElement(HomematicCommand command, HistoryStrategy strategy, BigDecimal valueDifferenceToSave) {
        this.command = command;
        this.strategy = strategy;
        this.valueDifferenceToSave = valueDifferenceToSave;
    }
}
