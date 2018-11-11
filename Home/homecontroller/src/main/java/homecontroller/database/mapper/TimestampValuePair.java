package homecontroller.database.mapper;

import java.time.LocalDateTime;

public class TimestampValuePair {

	private LocalDateTime timeatamp;

	public TimestampValuePair(LocalDateTime timeatamp, long value) {
		super();
		this.timeatamp = timeatamp;
		this.value = value;
	}

	private long value;

	public LocalDateTime getTimeatamp() {
		return timeatamp;
	}

	public void setTimeatamp(LocalDateTime timeatamp) {
		this.timeatamp = timeatamp;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}
}
