package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import de.fimatas.home.library.model.PersistentCacheKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class HeatpumpBasementModel extends AbstractSystemModel {

    private boolean busy;

    private boolean offline;

    private boolean standby;

    private ConditionColor conditionColor;

    private long apiReadTimestamp = 0;

    private List<HeatpumpBasementDatapoint> datapoints = new LinkedList<>();

    private Map<HeatpumpBasementDatapoints, PersistentCacheKey> historyDatapointsAndDevices = new HashMap<>();

    public HeatpumpBasementModel() {
        timestamp = System.currentTimeMillis();
    }

}
