package de.fimatas.home.library.domain.model;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public class ElectricVehicleState {

    public ElectricVehicleState(ElectricVehicle electricVehicle, short batteryPercentage, LocalDateTime timestamp) {
        this.electricVehicle = electricVehicle;
        this.batteryPercentage = batteryPercentage;
        this.timestamp = timestamp;
        this.additionalChargingPercentage = 0;
    }

    private final ElectricVehicle electricVehicle;

    private short batteryPercentage;

    private short additionalChargingPercentage;

    private boolean connectedToWallbox = false;

    private LocalDateTime timestamp;

    public LocalDateTime getTimestamp() {
        return timestamp;
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