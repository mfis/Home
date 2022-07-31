package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class HeatpumpModel implements Serializable {

    private Map<Place, Heatpump> heatpumpMap = new EnumMap<>(Place.class);

    private long timestamp;

    private boolean busy;

    public HeatpumpModel() {
        setTimestamp(System.currentTimeMillis());
    }

}
