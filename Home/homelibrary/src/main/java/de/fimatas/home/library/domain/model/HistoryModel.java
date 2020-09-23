package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class HistoryModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private long dateTime;

    private List<PowerConsumptionMonth> totalElectricPowerConsumptionMonth;

    private List<PowerConsumptionDay> totalElectricPowerConsumptionDay;

    private List<PowerConsumptionMonth> wallboxElectricPowerConsumptionMonth;

    private List<PowerConsumptionDay> wallboxElectricPowerConsumptionDay;

    private boolean initialized = false;

    private BigDecimal highestOutsideTemperatureInLast24Hours;

    private LinkedList<TemperatureHistory> outsideTemperature;

    private LinkedList<TemperatureHistory> bedRoomTemperature;

    private LinkedList<TemperatureHistory> kidsRoomTemperature;

    private LinkedList<TemperatureHistory> laundryTemperature;

    // ----------

    public void updateDateTime() {
        dateTime = new Date().getTime();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public LinkedList<TemperatureHistory> getOutsideTemperature() { // NOSONAR
        return outsideTemperature;
    }

    public void setOutsideTemperature(LinkedList<TemperatureHistory> outsideTemperature) { // NOSONAR
        this.outsideTemperature = outsideTemperature;
    }

    public HistoryModel() {
        super();
        updateDateTime();
        totalElectricPowerConsumptionMonth = new LinkedList<>();
        totalElectricPowerConsumptionDay = new LinkedList<>();
        wallboxElectricPowerConsumptionMonth = new LinkedList<>();
        wallboxElectricPowerConsumptionDay = new LinkedList<>();
        outsideTemperature = new LinkedList<>();
        bedRoomTemperature = new LinkedList<>();
        kidsRoomTemperature = new LinkedList<>();
        laundryTemperature = new LinkedList<>();
    }

    public long getDateTime() {
        return dateTime;
    }

    public BigDecimal getHighestOutsideTemperatureInLast24Hours() {
        return highestOutsideTemperatureInLast24Hours;
    }

    public void setHighestOutsideTemperatureInLast24Hours(BigDecimal highestOutsideTemperatureInLast24Hours) {
        this.highestOutsideTemperatureInLast24Hours = highestOutsideTemperatureInLast24Hours;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public LinkedList<TemperatureHistory> getBedRoomTemperature() {// NOSONAR
        return bedRoomTemperature;
    }

    public void setBedRoomTemperature(LinkedList<TemperatureHistory> bedRoomTemperature) {// NOSONAR
        this.bedRoomTemperature = bedRoomTemperature;
    }

    public LinkedList<TemperatureHistory> getKidsRoomTemperature() {// NOSONAR
        return kidsRoomTemperature;
    }

    public void setKidsRoomTemperature(LinkedList<TemperatureHistory> kidsRoomTemperature) {// NOSONAR
        this.kidsRoomTemperature = kidsRoomTemperature;
    }

    public LinkedList<TemperatureHistory> getLaundryTemperature() {// NOSONAR
        return laundryTemperature;
    }

    public void setLaundryTemperature(LinkedList<TemperatureHistory> laundryTemperature) {// NOSONAR
        this.laundryTemperature = laundryTemperature;
    }

    public List<PowerConsumptionMonth> getTotalElectricPowerConsumptionMonth() {
        return totalElectricPowerConsumptionMonth;
    }

    public void setTotalElectricPowerConsumptionMonth(List<PowerConsumptionMonth> totalElectricPowerConsumptionMonth) {
        this.totalElectricPowerConsumptionMonth = totalElectricPowerConsumptionMonth;
    }

    public List<PowerConsumptionDay> getTotalElectricPowerConsumptionDay() {
        return totalElectricPowerConsumptionDay;
    }

    public void setTotalElectricPowerConsumptionDay(List<PowerConsumptionDay> totalElectricPowerConsumptionDay) {
        this.totalElectricPowerConsumptionDay = totalElectricPowerConsumptionDay;
    }

    public List<PowerConsumptionMonth> getWallboxElectricPowerConsumptionMonth() {
        return wallboxElectricPowerConsumptionMonth;
    }

    public void setWallboxElectricPowerConsumptionMonth(List<PowerConsumptionMonth> wallboxElectricPowerConsumptionMonth) {
        this.wallboxElectricPowerConsumptionMonth = wallboxElectricPowerConsumptionMonth;
    }

    public List<PowerConsumptionDay> getWallboxElectricPowerConsumptionDay() {
        return wallboxElectricPowerConsumptionDay;
    }

    public void setWallboxElectricPowerConsumptionDay(List<PowerConsumptionDay> wallboxElectricPowerConsumptionDay) {
        this.wallboxElectricPowerConsumptionDay = wallboxElectricPowerConsumptionDay;
    }

}
