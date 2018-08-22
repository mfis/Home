package home.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class HistoryModel implements Serializable {

	private static final long serialVersionUID = 1L;

	private long dateTime;

	private Map<Long, BigDecimal> monthlyPowerConsumption;

	// ----------

	public HistoryModel() {
		super();
		dateTime = new Date().getTime();
		monthlyPowerConsumption = new LinkedHashMap<>();
	}

	public long getDateTime() {
		return dateTime;
	}

	public Map<Long, BigDecimal> getMonthlyPowerConsumption() {
		return monthlyPowerConsumption;
	}

	public void setMonthlyPowerConsumption(Map<Long, BigDecimal> monthlyPowerConsumption) {
		this.monthlyPowerConsumption = monthlyPowerConsumption;
	}
}
