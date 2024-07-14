package de.fimatas.home.library.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PvAdditionalDataModel {

    private long dateTime;

    private int batteryStateOfCharge;

    private BigDecimal batteryCapacity;

    private PvBatteryState pvBatteryState;

    private int wattage;
}
