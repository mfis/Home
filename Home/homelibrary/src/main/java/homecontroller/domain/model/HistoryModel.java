package homecontroller.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;

public class HistoryModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private long dateTime;

	private LinkedList<PowerConsumptionMonth> electricPowerConsumption;

	private boolean electricPowerConsumptionInitialized = false;

	private BigDecimal highestOutsideTemperatureInLast24Hours;

	// ----------

	public HistoryModel() {
		super();
		dateTime = new Date().getTime();
		electricPowerConsumption = new LinkedList<>();
	}

	public long getDateTime() {
		return dateTime;
	}

	public LinkedList<PowerConsumptionMonth> getElectricPowerConsumption() {
		return electricPowerConsumption;
	}

	public void setElectricPowerConsumption(LinkedList<PowerConsumptionMonth> electricPowerConsumption) {
		this.electricPowerConsumption = electricPowerConsumption;
	}

	public boolean isElectricPowerConsumptionInitialized() {
		return electricPowerConsumptionInitialized;
	}

	public void setElectricPowerConsumptionInitialized(boolean electricPowerConsumptionInitialized) {
		this.electricPowerConsumptionInitialized = electricPowerConsumptionInitialized;
	}

	public BigDecimal getHighestOutsideTemperatureInLast24Hours() {
		return highestOutsideTemperatureInLast24Hours;
	}

	public void setHighestOutsideTemperatureInLast24Hours(BigDecimal highestOutsideTemperatureInLast24Hours) {
		this.highestOutsideTemperatureInLast24Hours = highestOutsideTemperatureInLast24Hours;
	}

}
