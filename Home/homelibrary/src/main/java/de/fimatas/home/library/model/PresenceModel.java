package de.fimatas.home.library.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class PresenceModel {

    private long dateTime;

    private Map<String, PresenceState> presenceStates = new HashMap<>();

}
