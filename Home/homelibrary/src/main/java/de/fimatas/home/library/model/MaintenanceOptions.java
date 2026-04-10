package de.fimatas.home.library.model;

import lombok.Getter;

@Getter
public enum MaintenanceOptions {

    TEST_PUSH(ConditionColor.GREEN),
    TEST_PUSH_WITH_HINT(ConditionColor.GREEN),
    REFRESH_MODELS(ConditionColor.ORANGE),
    REBOOT_CONTROLLER(ConditionColor.RED),
    ;

    private final ConditionColor conditionColor;

    MaintenanceOptions(ConditionColor conditionColor){
        this.conditionColor = conditionColor;
    }
}
