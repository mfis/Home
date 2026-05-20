package de.fimatas.home.controller.command;

import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static de.fimatas.home.controller.command.HomematicCommandConstants.EMPTY;

@Getter
@Setter
@NoArgsConstructor
public class HomematicCommand extends AbstractCommand {

    private HomematicCommandType commandType;

    private Device device;

    private Datapoint datapoint;

    private String suffix = EMPTY;

    private Boolean stateToSet;

    private String stringToSet;

    public String id() {
        return varName;
    }

    public boolean isProgramRunCommand() {
        return commandType == HomematicCommandType.RUN_PROGRAM;
    }
}
