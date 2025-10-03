package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class HeatpumpBasementModel extends AbstractSystemModel {

    private boolean busy;

    private boolean offline;

    private ConditionColor conditionColor;

    private long apiReadTimestamp = 0;

    private final List<HeatpumpBasementDatapoint> datapoints = new LinkedList<>();

    public HeatpumpBasementModel() {
        timestamp = System.currentTimeMillis();
    }

}
