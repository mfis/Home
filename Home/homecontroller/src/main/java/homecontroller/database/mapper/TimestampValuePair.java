package homecontroller.database.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import homecontroller.model.HistoryValueType;

public class TimestampValuePair {

	private LocalDateTime timestamp;

	private BigDecimal value;

	private HistoryValueType type;

	public TimestampValuePair(LocalDateTime timestamp, BigDecimal value, HistoryValueType type) {
		super();
		this.timestamp = timestamp;
		this.value = value;
		this.type = type;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public BigDecimal getValue() {
		return value;
	}

	public HistoryValueType getType() {
		return type;
	}

}
