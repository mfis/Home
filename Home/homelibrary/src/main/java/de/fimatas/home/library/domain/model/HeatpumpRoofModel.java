package de.fimatas.home.library.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.EnumMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class HeatpumpRoofModel extends AbstractSystemModel {

    @Serial
    private static final long serialVersionUID = 1L;

    private Map<Place, HeatpumpRoof> heatpumpMap = new EnumMap<>(Place.class);

    private boolean busy;

    private String name;

    public HeatpumpRoofModel() {
        timestamp = System.currentTimeMillis();
    }

}
