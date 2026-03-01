package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.ConditionColor;
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

    private ConditionColor conditionColor;

    private long apiReadTimestamp = 0;

    private final List<HeatpumpBasementDatapoint> datapoints = new LinkedList<>();

    private Map<String, Device> historyIdsAndDevices = new HashMap<>();

    public HeatpumpBasementModel() {
        timestamp = System.currentTimeMillis();
    }

}
