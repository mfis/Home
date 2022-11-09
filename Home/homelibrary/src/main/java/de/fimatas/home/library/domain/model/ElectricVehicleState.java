package de.fimatas.home.library.domain.model;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public class ElectricVehicleState {

    public ElectricVehicleState(ElectricVehicle electricVehicle, short batteryPercentage, LocalDateTime batteryPercentageTimestamp) {
        this.electricVehicle = electricVehicle;
        this.batteryPercentage = batteryPercentage;
        this.batteryPercentageTimestamp = batteryPercentageTimestamp;
        this.additionalChargingPercentage = 0;
        this.connectedToWallbox = false;
        this.activeCharging = false;
    }

    private final ElectricVehicle electricVehicle;

    private short batteryPercentage;

    private short additionalChargingPercentage;

    private boolean connectedToWallbox;

    private boolean activeCharging;

    private LocalDateTime batteryPercentageTimestamp;

    private LocalDateTime chargingTimestamp;

    public LocalDateTime getChargingTimestamp() {
        return chargingTimestamp;
    }

    public void setChargingTimestamp(LocalDateTime chargingTimestamp) {
        this.chargingTimestamp = chargingTimestamp;
    }

    public boolean isActiveCharging() {
        return activeCharging;
    }

    public void setActiveCharging(boolean activeCharging) {
        this.activeCharging = activeCharging;
    }

    public LocalDateTime getBatteryPercentageTimestamp() {
        return batteryPercentageTimestamp;
    }

    public short getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(short batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public ElectricVehicle getElectricVehicle() {
        return electricVehicle;
    }

    public boolean isConnectedToWallbox() {
        return connectedToWallbox;
    }

    public void setConnectedToWallbox(boolean connectedToWallbox) {
        this.connectedToWallbox = connectedToWallbox;
    }

    public short getAdditionalChargingPercentage() {
        return additionalChargingPercentage;
    }

    public void setAdditionalChargingPercentage(short additionalChargingPercentage) {
        this.additionalChargingPercentage = additionalChargingPercentage;
    }
}
