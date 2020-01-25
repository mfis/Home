package homecontroller.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

public class PowerConsumptionDay implements Serializable {

	private static final long serialVersionUID = 1L;

	private transient LocalDateTime measurePointMaxDateTime = null;

	private long measurePointMax;

	// private Map<TimeRange, BigDecimal> lastSingleValues;

	private Map<TimeRange, BigDecimal> values;

	public PowerConsumptionDay() {
		// lastSingleValues = new LinkedHashMap<>();
		values = new LinkedHashMap<>();
	}

	public LocalDateTime measurePointMaxDateTime() {
		if (measurePointMaxDateTime == null) {
			measurePointMaxDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(measurePointMax),
					ZoneId.systemDefault());
		}
		return measurePointMaxDateTime;
	}

	public LocalDateTime getMeasurePointMaxDateTime() {
		return measurePointMaxDateTime;
	}

	public void setMeasurePointMaxDateTime(LocalDateTime measurePointMaxDateTime) {
		this.measurePointMaxDateTime = measurePointMaxDateTime;
	}

	public long getMeasurePointMax() {
		return measurePointMax;
	}

	public void setMeasurePointMax(long measurePointMax) {
		this.measurePointMax = measurePointMax;
		measurePointMaxDateTime = null;
	}

	public Map<TimeRange, BigDecimal> getValues() {
		return values;
	}

	public void setValues(Map<TimeRange, BigDecimal> values) {
		this.values = values;
	}

	// public Map<TimeRange, BigDecimal> getLastSingleValues() {
	// return lastSingleValues;
	// }
	//
	// public void setLastSingleValues(Map<TimeRange, BigDecimal>
	// lastSingleValues) {
	// this.lastSingleValues = lastSingleValues;
	// }

}
