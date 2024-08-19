package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.AbstractSystemModel;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class PresenceModel extends AbstractSystemModel {

    private Map<String, PresenceState> presenceStates = new HashMap<>();

}
