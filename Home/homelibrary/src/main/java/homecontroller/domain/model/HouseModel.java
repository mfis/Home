package homecontroller.domain.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class HouseModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private long dateTime;

	private RoomClimate climateKidsRoom;

	private RoomClimate climateBathRoom;

	private RoomClimate climateBedRoom;

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

	public List<RoomClimate> lookupRooms() {

		Field[] fields = this.getClass().getDeclaredFields();
		List<RoomClimate> results = new LinkedList<RoomClimate>();
		try {
			for (Field field : fields) {
				if (field.getType().equals(RoomClimate.class)) {
					results.add((RoomClimate) field.get(this));
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("Exception collecting RoomClimate's:", e);
		}
		return results;
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

}
