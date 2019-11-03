package homecontroller.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class HistoryModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private long dateTime;

	private List<PowerConsumptionMonth> electricPowerConsumption;

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
		electricPowerConsumption = new LinkedList<>();
		outsideTemperature = new LinkedList<>();
		bedRoomTemperature = new LinkedList<>();
		kidsRoomTemperature = new LinkedList<>();
		laundryTemperature = new LinkedList<>();
	}

	public long getDateTime() {
		return dateTime;
	}

	public List<PowerConsumptionMonth> getElectricPowerConsumption() {
		return electricPowerConsumption;
	}

	public void setElectricPowerConsumption(List<PowerConsumptionMonth> electricPowerConsumption) {
		this.electricPowerConsumption = electricPowerConsumption;
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

}
