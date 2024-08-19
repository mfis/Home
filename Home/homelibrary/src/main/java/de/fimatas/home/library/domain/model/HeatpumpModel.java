package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class HeatpumpModel extends AbstractSystemModel {

    private Map<Place, Heatpump> heatpumpMap = new EnumMap<>(Place.class);

    private boolean busy;

    public HeatpumpModel() {
        timestamp = System.currentTimeMillis();
    }

}
