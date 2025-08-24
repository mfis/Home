package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.AbstractSystemModel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class PvAdditionalDataModel extends AbstractSystemModel {

    private long lastCollectionTimeReadMillis;

    private PhotovoltaicsStringsStatus stringsStatus = PhotovoltaicsStringsStatus.UNKNOWN;

    private Map<String, String> detailInfos = new LinkedHashMap<>();

    private String alarm;

    private int batteryStateOfCharge; // percent SOC

    private BigDecimal batteryCapacity; // kW/h

    private PvBatteryState pvBatteryState; // charging/discharging

    private int batteryWattage; // charging/feeding 'pvBatteryState' Watt

    private int productionWattage;

    private int consumptionWattage;

    private int maxChargeWattage; // maximum watt to charge

    private int minChargingWattageForOverflowControl; // keep left this wattage for charging if target percentage is not reached

    private int batteryPercentageEmptyForOverflowControl; // minimum percent SOC
}
