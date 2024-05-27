package de.fimatas.home.library.model;

import lombok.Getter;

@Getter
public enum TaskState {
    IN_RANGE(ConditionColor.GREEN, "in"),
    NEAR_BEFORE_EXECUTION(ConditionColor.GREEN, "in"),
    LITTLE_OUT_OF_RANGE(ConditionColor.ORANGE, "seit"),
    FAR_OUT_OF_RANGE(ConditionColor.RED, "seit"),
    UNKNOWN(ConditionColor.RED, "unbekannt"),
    ;

    private final ConditionColor conditionColor;
    private final String statePrefix;

    TaskState(ConditionColor conditionColor, String statePrefix) {
        this.conditionColor = conditionColor;
        this.statePrefix = statePrefix;
    }
}
