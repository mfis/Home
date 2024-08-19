package de.fimatas.home.library.model;

import lombok.Getter;

@Getter
public enum MaintenanceOptions {

    REFRESH_MODELS(ConditionColor.ORANGE),
    CONTROLLER_REBOOT(ConditionColor.RED),
    ;

    private final ConditionColor conditionColor;

    MaintenanceOptions(ConditionColor conditionColor){
        this.conditionColor = conditionColor;
    }
}
