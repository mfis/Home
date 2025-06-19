package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.util.EnumMap;
import java.util.Map;

@Data
public class HeatpumpRoofModel extends AbstractSystemModel {

    private Map<Place, HeatpumpRoof> heatpumpMap = new EnumMap<>(Place.class);

    private boolean busy;

    private String name;

    public HeatpumpRoofModel() {
        timestamp = System.currentTimeMillis();
    }

}
