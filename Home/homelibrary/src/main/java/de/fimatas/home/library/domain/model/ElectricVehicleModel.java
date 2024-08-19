package de.fimatas.home.library.domain.model;

import lombok.Getter;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
public class ElectricVehicleModel extends AbstractSystemModel{

    private Map<ElectricVehicle, ElectricVehicleState> evMap = new EnumMap<>(ElectricVehicle.class);

    public ElectricVehicleModel() {
        setTimestamp(System.currentTimeMillis());
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
