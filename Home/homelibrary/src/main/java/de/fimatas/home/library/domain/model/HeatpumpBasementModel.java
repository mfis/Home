package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class HeatpumpBasementModel extends AbstractSystemModel {

    private boolean busy;

    private final List<HeatpumpBasementDatapoint> datapoints = new LinkedList<>();

    public HeatpumpBasementModel() {
        timestamp = System.currentTimeMillis();
    }

}
