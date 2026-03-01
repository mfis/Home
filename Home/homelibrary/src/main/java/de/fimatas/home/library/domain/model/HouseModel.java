package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.annotation.EnableHomekit;
import de.fimatas.home.library.annotation.EnablePhotovoltaicsOverflow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Field;
import java.util.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class HouseModel extends AbstractSystemModel {

    private RoomClimate climateRoof;

    @EnableHomekit(accessoryId = 1001)
    private RoomClimate climateKidsRoom1;

    @EnableHomekit(accessoryId = 1002)
    private RoomClimate climateKidsRoom2;

    @EnableHomekit(accessoryId = 1003)
    private RoomClimate climateBathRoom;

    private Heating heatingBathRoom;

    @EnableHomekit(accessoryId = 1004)
    private RoomClimate climateBedRoom;

    private Shutter leftWindowBedRoom;

    @EnableHomekit(accessoryId = 1005)
    private RoomClimate climateLivingRoom;

    @EnableHomekit(accessoryId = 1006)
    private RoomClimate climateLaundry;

    private OutdoorClimate climateGarden;

    private OutdoorClimate climateEntrance;

    private Switch kitchenWindowLightSwitch;

    @EnablePhotovoltaicsOverflow
    private Switch wallboxSwitch;

    private Switch workshopVentilationSwitch;

    @EnableHomekit(accessoryId = 1007)
    private RoomClimate climateGuestRoom;

    private Heating heatingGuestRoom;

    private WindowSensor guestRoomWindowSensor;

    @EnablePhotovoltaicsOverflow
    private Switch guestRoomInfraredHeater;

    private WindowSensor workshopWindowSensor;

    private Switch workshopLightSwitch;

    @EnableHomekit(accessoryId = 1009)
    private RoomClimate climateWorkshop;

    private WindowSensor laundryWindowSensor;

    private PowerMeter gridElectricalPower;

    private PowerMeter producedElectricalPower;

    private PowerMeter consumedElectricalPower;

    private long pvStatusTime;

    private long gridElectricStatusTime;

    private PowerMeter wallboxElectricalPowerConsumption;

    private Doorbell frontDoorBell;

    private Doorlock frontDoorLock;

    private List<String> lowBatteryDevices;

    private List<String> warnings;

    private Map<Place, String> placeSubtitles;

    // ----------

    @EnableHomekit(accessoryId = 1008)
    private OutdoorClimate conclusionClimateFacadeMin;

    private OutdoorClimate conclusionClimateFacadeMax;

    // ----------

    public HouseModel() {
        super();
        timestamp = new Date().getTime();
        lowBatteryDevices = new LinkedList<>();
        warnings = new LinkedList<>();
        placeSubtitles = new LinkedHashMap<>();
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

    @SuppressWarnings("unchecked")
    public <T> T lookupField(Field field, Class<T> clazz) {
        try {
            return (T) field.get(this);
        } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
            throw new IllegalArgumentException("Exception reading field '" + field + " of class" + clazz + "':", e);
        }
    }
}
