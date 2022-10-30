package de.fimatas.home.library.domain.model;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ElectricVehicleModel {

    private Map<ElectricVehicle, ElectricVehicleState> evMap = new EnumMap<>(ElectricVehicle.class);

    private long timestamp;

    public ElectricVehicleModel() {
        setTimestamp(System.currentTimeMillis());
    }

    public Map<ElectricVehicle, ElectricVehicleState> getEvMap() {
        return evMap;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
