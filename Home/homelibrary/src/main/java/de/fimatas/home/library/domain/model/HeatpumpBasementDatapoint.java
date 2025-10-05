package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class HeatpumpBasementDatapoint extends AbstractSystemModel {

    private String id;

    private String name;

    private ValueWithTendency<BigDecimal> valueWithTendency;

    private String valueFormattedLong;

    private String valueFormattedShort;

    private boolean stateOff;

    private int group;

    private String description;

    private ConditionColor conditionColor;

    public HeatpumpBasementDatapoint() {
        timestamp = System.currentTimeMillis();
    }
    
}
