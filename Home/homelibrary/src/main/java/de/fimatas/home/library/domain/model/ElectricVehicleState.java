package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class ElectricVehicleState {

    public ElectricVehicleState(ElectricVehicle electricVehicle, short batteryPercentage, LocalDateTime batteryPercentageTimestamp) {
        this.electricVehicle = electricVehicle;
        this.batteryPercentage = batteryPercentage;
        this.batteryPercentageTimestamp = batteryPercentageTimestamp;
        this.chargeLimit = null;
        this.additionalChargingPercentage = 0;
        this.connectedToWallbox = false;
        this.activeCharging = false;
        this.chargingTime = new LinkedList<>();
        this.chargingCapacity = null;
    }

    private final ElectricVehicle electricVehicle;

    private short batteryPercentage;

    private short additionalChargingPercentage;

    private boolean connectedToWallbox;

    private boolean activeCharging;

    private ChargeLimit chargeLimit;

    private LocalDateTime batteryPercentageTimestamp;

    private LocalDateTime chargingTimestamp;

    private BigDecimal chargingCapacity;

    private List<EvChargingTime> chargingTime;
}
