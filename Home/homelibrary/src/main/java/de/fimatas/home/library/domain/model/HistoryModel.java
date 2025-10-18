package de.fimatas.home.library.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class HistoryModel extends AbstractSystemModel {

    private List<PowerConsumptionMonth> purchasedElectricPowerConsumptionMonth;

    private List<PowerConsumptionDay> purchasedElectricPowerConsumptionDay;

    private List<PowerConsumptionMonth> feedElectricPowerConsumptionMonth;

    private List<PowerConsumptionDay> feedElectricPowerConsumptionDay;

    private List<PowerConsumptionMonth> producedElectricPowerMonth;

    private List<PowerConsumptionDay> producedElectricPowerDay;

    private List<PowerConsumptionMonth> selfusedElectricPowerConsumptionMonth;

    private List<PowerConsumptionDay> selfusedElectricPowerConsumptionDay;

    private List<PowerConsumptionMonth> wallboxElectricPowerConsumptionMonth;

    private List<PowerConsumptionDay> wallboxElectricPowerConsumptionDay;

    private List<PowerConsumptionMonth> gasConsumptionMonth;

    private List<PowerConsumptionDay> gasConsumptionDay;

    private List<PowerConsumptionMonth> heatpumpBasementElectricPowerConsumptionMonth;

    private List<PowerConsumptionDay> heatpumpBasementElectricPowerConsumptionDay;

    private boolean initialized = false;

    private BigDecimal highestOutsideTemperatureInLast24Hours;

    private LinkedList<TemperatureHistory> outsideTemperature;

    private LinkedList<TemperatureHistory> bedRoomTemperature;

    private LinkedList<TemperatureHistory> kidsRoom1Temperature;

    private LinkedList<TemperatureHistory> kidsRoom2Temperature;

    private LinkedList<TemperatureHistory> laundryTemperature;

    // ----------

    public void updateDateTime() {
        timestamp = new Date().getTime();
    }

    public HistoryModel() {
        super();
        updateDateTime();
        purchasedElectricPowerConsumptionMonth = new LinkedList<>();
        purchasedElectricPowerConsumptionDay = new LinkedList<>();
        feedElectricPowerConsumptionMonth = new LinkedList<>();
        feedElectricPowerConsumptionDay = new LinkedList<>();
        producedElectricPowerMonth  = new LinkedList<>();
        producedElectricPowerDay = new LinkedList<>();
        selfusedElectricPowerConsumptionMonth = new LinkedList<>();
        selfusedElectricPowerConsumptionDay = new LinkedList<>();
        wallboxElectricPowerConsumptionMonth = new LinkedList<>();
        wallboxElectricPowerConsumptionDay = new LinkedList<>();
        heatpumpBasementElectricPowerConsumptionMonth = new LinkedList<>();
        heatpumpBasementElectricPowerConsumptionDay = new LinkedList<>();
        gasConsumptionDay = new LinkedList<>();
        gasConsumptionMonth = new LinkedList<>();
        outsideTemperature = new LinkedList<>();
        bedRoomTemperature = new LinkedList<>();
        kidsRoom1Temperature = new LinkedList<>();
        kidsRoom2Temperature = new LinkedList<>();
        laundryTemperature = new LinkedList<>();
    }
}
