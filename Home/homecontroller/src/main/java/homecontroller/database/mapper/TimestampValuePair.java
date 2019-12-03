package homecontroller.database.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TimestampValuePair {

	private LocalDateTime timestamp;

	public TimestampValuePair(LocalDateTime timestamp, BigDecimal value) {
		super();
		this.timestamp = timestamp;
		this.value = value;
	}

	private BigDecimal value;

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}
}
