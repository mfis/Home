package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.AbstractSystemModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class PresenceModel extends AbstractSystemModel {

    private Map<String, PresenceState> presenceStates = new HashMap<>();

}
