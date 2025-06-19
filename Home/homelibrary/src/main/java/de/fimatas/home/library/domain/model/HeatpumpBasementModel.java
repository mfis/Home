package de.fimatas.home.library.domain.model;

import lombok.Data;

@Data
public class HeatpumpBasementModel extends AbstractSystemModel {

    private boolean busy;

    public HeatpumpBasementModel() {
        timestamp = System.currentTimeMillis();
    }

}
