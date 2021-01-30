package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HouseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private long dateTime;

    private RoomClimate climateKidsRoom;

    private RoomClimate climateBathRoom;

    private Heating heatingBathRoom;

    private RoomClimate climateBedRoom;

    private Shutter leftWindowBedRoom;

    private RoomClimate climateLivingRoom;

    private RoomClimate climateLaundry;

    private OutdoorClimate climateGarden;

    private OutdoorClimate climateTerrace;

    private OutdoorClimate climateEntrance;

    private Switch kitchenWindowLightSwitch;

    private Switch wallboxSwitch;

    private Switch workshopVentilationSwitch;

    private WindowSensor guestRoomWindowSensor;

    private WindowSensor workshopWindowSensor;

    private WindowSensor laundryWindowSensor;

    private PowerMeter totalElectricalPowerConsumption;

    private PowerMeter wallboxElectricalPowerConsumption;

    private Doorbell frontDoorBell;

    private Camera frontDoorCamera;

    private Doorlock frontDoorLock;

    private List<String> lowBatteryDevices;

    private List<String> warnings;

    // ----------

    private OutdoorClimate conclusionClimateFacadeMin;

    private OutdoorClimate conclusionClimateFacadeMax;

    // ----------

    public HouseModel() {
        super();
        dateTime = new Date().getTime();
        lowBatteryDevices = new LinkedList<>();
        warnings = new LinkedList<>();
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> lookupFields(Class<T> clazz) {

        Field[] fields = this.getClass().getDeclaredFields();
        Map<String, T> results = new HashMap<>();
        try {
            for (Field field : fields) {
                if ((field.getType().equals(clazz)
                    || (field.getType().getSuperclass() != null && field.getType().getSuperclass().equals(clazz)))
                    && field.get(this) != null) {
                    results.put(field.getName(), (T) field.get(this));
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalArgumentException("Exception collecting fields:", e);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public <T> T lookupField(String field, Class<T> clazz) {
        try {
            return (T) this.getClass().getDeclaredField(field).get(this);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException("Exception reading field '" + field + " of class" + clazz + "':", e);
        }

    }

    public long getDateTime() {
        return dateTime;
    }

    public Switch getKitchenWindowLightSwitch() {
        return kitchenWindowLightSwitch;
    }

    public void setKitchenWindowLightSwitch(Switch kitchenWindowLightSwitch) {
        this.kitchenWindowLightSwitch = kitchenWindowLightSwitch;
    }

    public List<String> getLowBatteryDevices() {
        return lowBatteryDevices;
    }

    public void setLowBatteryDevices(List<String> lowBatteryDevices) {
        this.lowBatteryDevices = lowBatteryDevices;
    }

    public RoomClimate getClimateKidsRoom() {
        return climateKidsRoom;
    }

    public void setClimateKidsRoom(RoomClimate climateKidsRoom) {
        this.climateKidsRoom = climateKidsRoom;
    }

    public RoomClimate getClimateBathRoom() {
        return climateBathRoom;
    }

    public void setClimateBathRoom(RoomClimate climateBathRoom) {
        this.climateBathRoom = climateBathRoom;
    }

    public RoomClimate getClimateBedRoom() {
        return climateBedRoom;
    }

    public void setClimateBedRoom(RoomClimate climateBedRoom) {
        this.climateBedRoom = climateBedRoom;
    }

    public RoomClimate getClimateLivingRoom() {
        return climateLivingRoom;
    }

    public void setClimateLivingRoom(RoomClimate climateLivingRoom) {
        this.climateLivingRoom = climateLivingRoom;
    }

    public OutdoorClimate getClimateTerrace() {
        return climateTerrace;
    }

    public void setClimateTerrace(OutdoorClimate climateTerrace) {
        this.climateTerrace = climateTerrace;
    }

    public OutdoorClimate getClimateEntrance() {
        return climateEntrance;
    }

    public void setClimateEntrance(OutdoorClimate climateEntrance) {
        this.climateEntrance = climateEntrance;
    }

    public OutdoorClimate getConclusionClimateFacadeMin() {
        return conclusionClimateFacadeMin;
    }

    public void setConclusionClimateFacadeMin(OutdoorClimate conclusionClimateFacadeMin) {
        this.conclusionClimateFacadeMin = conclusionClimateFacadeMin;
    }

    public OutdoorClimate getConclusionClimateFacadeMax() {
        return conclusionClimateFacadeMax;
    }

    public void setConclusionClimateFacadeMax(OutdoorClimate conclusionClimateFacadeMax) {
        this.conclusionClimateFacadeMax = conclusionClimateFacadeMax;
    }

    public Shutter getLeftWindowBedRoom() {
        return leftWindowBedRoom;
    }

    public void setLeftWindowBedRoom(Shutter leftWindowBedRoom) {
        this.leftWindowBedRoom = leftWindowBedRoom;
    }

    public Heating getHeatingBathRoom() {
        return heatingBathRoom;
    }

    public void setHeatingBathRoom(Heating heatingBathRoom) {
        this.heatingBathRoom = heatingBathRoom;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public RoomClimate getClimateLaundry() {
        return climateLaundry;
    }

    public void setClimateLaundry(RoomClimate climateLaundry) {
        this.climateLaundry = climateLaundry;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public Doorbell getFrontDoorBell() {
        return frontDoorBell;
    }

    public void setFrontDoorBell(Doorbell frontDoorBell) {
        this.frontDoorBell = frontDoorBell;
    }

    public Camera getFrontDoorCamera() {
        return frontDoorCamera;
    }

    public void setFrontDoorCamera(Camera frontDoorCamera) {
        this.frontDoorCamera = frontDoorCamera;
    }

    public Doorlock getFrontDoorLock() {
        return frontDoorLock;
    }

    public void setFrontDoorLock(Doorlock frontDoorLock) {
        this.frontDoorLock = frontDoorLock;
    }

    public OutdoorClimate getClimateGarden() {
        return climateGarden;
    }

    public void setClimateGarden(OutdoorClimate climateGarden) {
        this.climateGarden = climateGarden;
    }

    public Switch getWallboxSwitch() {
        return wallboxSwitch;
    }

    public void setWallboxSwitch(Switch wallboxSwitch) {
        this.wallboxSwitch = wallboxSwitch;
    }

    public PowerMeter getTotalElectricalPowerConsumption() {
        return totalElectricalPowerConsumption;
    }

    public void setTotalElectricalPowerConsumption(PowerMeter totalElectricalPowerConsumption) {
        this.totalElectricalPowerConsumption = totalElectricalPowerConsumption;
    }

    public PowerMeter getWallboxElectricalPowerConsumption() {
        return wallboxElectricalPowerConsumption;
    }

    public void setWallboxElectricalPowerConsumption(PowerMeter wallboxElectricalPowerConsumption) {
        this.wallboxElectricalPowerConsumption = wallboxElectricalPowerConsumption;
    }

    public Switch getWorkshopVentilationSwitch() {
        return workshopVentilationSwitch;
    }

    public void setWorkshopVentilationSwitch(Switch workshopVentilationSwitch) {
        this.workshopVentilationSwitch = workshopVentilationSwitch;
    }

    public WindowSensor getGuestRoomWindowSensor() {
        return guestRoomWindowSensor;
    }

    public void setGuestRoomWindowSensor(WindowSensor guestRoomWindowSensor) {
        this.guestRoomWindowSensor = guestRoomWindowSensor;
    }

    public WindowSensor getWorkshopWindowSensor() {
        return workshopWindowSensor;
    }

    public void setWorkshopWindowSensor(WindowSensor workshopWindowSensor) {
        this.workshopWindowSensor = workshopWindowSensor;
    }

    public WindowSensor getLaundryWindowSensor() {
        return laundryWindowSensor;
    }

    public void setLaundryWindowSensor(WindowSensor laundryWindowSensor) {
        this.laundryWindowSensor = laundryWindowSensor;
    }

}
