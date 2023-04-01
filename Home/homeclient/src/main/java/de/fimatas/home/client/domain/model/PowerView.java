package de.fimatas.home.client.domain.model;

import de.fimatas.home.library.homematic.model.Device;

public class PowerView extends View {

    private String tendencyIcon = "";

    private String description = "";

    private ChartEntry todayConsumption;

    private Device device;

    public String getTendencyIcon() {
        return tendencyIcon;
    }

    public void setTendencyIcon(String tendencyIcon) {
        this.tendencyIcon = tendencyIcon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ChartEntry getTodayConsumption() {
        return todayConsumption;
    }

    public void setTodayConsumption(ChartEntry todayConsumption) {
        this.todayConsumption = todayConsumption;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
