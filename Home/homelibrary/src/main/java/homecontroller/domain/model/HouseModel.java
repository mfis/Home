package homecontroller.domain.model;

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

	private HeatingModel heatingBathRoom;

	private RoomClimate climateBedRoom;

	private Window leftWindowBedRoom;

	private RoomClimate climateLivingRoom;

	private OutdoorClimate climateTerrace;

	private OutdoorClimate climateEntrance;

	private SwitchModel kitchenWindowLightSwitch;

	private PowerMeterModel electricalPowerConsumption;

	private List<String> lowBatteryDevices;

	// ----------

	private OutdoorClimate conclusionClimateFacadeMin;

	private OutdoorClimate conclusionClimateFacadeMax;

	// ----------

	public HouseModel() {
		super();
		dateTime = new Date().getTime();
		lowBatteryDevices = new LinkedList<>();
	}

	@SuppressWarnings("unchecked")
	public <T> Map<String, T> lookupFields(Class<T> clazz) {

		Field[] fields = this.getClass().getDeclaredFields();
		Map<String, T> results = new HashMap<>();
		try {
			for (Field field : fields) {
				if ((field.getType().equals(clazz) || (field.getType().getSuperclass() != null
						&& field.getType().getSuperclass().equals(clazz))) && field.get(this) != null) {
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
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
				| SecurityException e) {
			throw new IllegalArgumentException(
					"Exception reading field '" + field + " of class" + clazz + "':", e);
		}

	}

	public long getDateTime() {
		return dateTime;
	}

	public SwitchModel getKitchenWindowLightSwitch() {
		return kitchenWindowLightSwitch;
	}

	public void setKitchenWindowLightSwitch(SwitchModel kitchenWindowLightSwitch) {
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

	public PowerMeterModel getElectricalPowerConsumption() {
		return electricalPowerConsumption;
	}

	public void setElectricalPowerConsumption(PowerMeterModel electricalPowerConsumption) {
		this.electricalPowerConsumption = electricalPowerConsumption;
	}

	public Window getLeftWindowBedRoom() {
		return leftWindowBedRoom;
	}

	public void setLeftWindowBedRoom(Window leftWindowBedRoom) {
		this.leftWindowBedRoom = leftWindowBedRoom;
	}

	public HeatingModel getHeatingBathRoom() {
		return heatingBathRoom;
	}

	public void setHeatingBathRoom(HeatingModel heatingBathRoom) {
		this.heatingBathRoom = heatingBathRoom;
	}

}
