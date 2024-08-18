package de.fimatas.home.library.model;

import lombok.Getter;

@Getter
public enum RepairValues {

    REFRESH_MODELS(ConditionColor.ORANGE),
    CONTROLLER_REBOOT(ConditionColor.RED),
    ;

    private final ConditionColor conditionColor;

    RepairValues(ConditionColor conditionColor){
        this.conditionColor = conditionColor;
    }
}
