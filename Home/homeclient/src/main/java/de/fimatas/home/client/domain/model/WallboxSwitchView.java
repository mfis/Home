package de.fimatas.home.client.domain.model;

import java.util.LinkedList;
import java.util.List;

public class WallboxSwitchView extends SwitchView {

    private List<ElectroVehicleView> evs = new LinkedList<>();

    public List<ElectroVehicleView> getEvs() {
        return evs;
    }
}
