package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class HeatpumpBasementDatapoint extends AbstractSystemModel {

    private String id;

    private String name;

    private String value;

    private String description;

    private ConditionColor conditionColor;

    public HeatpumpBasementDatapoint() {
        timestamp = System.currentTimeMillis();
    }
    
}
