package homecontroller.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public enum TimeRange {

	MORGING("in (6,7,8,9)", //
			Arrays.asList(6, 7, 8, 9)), //

	DAY("in (10,11,12,13,14,15,16,17,18)", //
			Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18)), //

	EVENING("in (19,20,21,22, 23)", //
			Arrays.asList(19, 20, 21, 22, 23)), //

	NIGHT("in (0,1,2,3,4,5)", //
			Arrays.asList(0, 1, 2, 3, 4, 5)), //
	;

	private final String hoursSqlQueryString;

	private final List<Integer> hoursIntList;

	private TimeRange(String hoursSqlQueryString, List<Integer> hoursIntList) {
		this.hoursSqlQueryString = hoursSqlQueryString;
		this.hoursIntList = hoursIntList;
	}

	public static TimeRange fromDateTime(LocalDateTime localDateTime) {
		int hours = localDateTime.getHour();
		for (TimeRange range : values()) {
			if (range.getHoursIntList().contains(hours)) {
				return range;
			}
		}
		throw new IllegalStateException("fromDateTime: unexpected hour: " + hours);
	}

	public String getHoursSqlQueryString() {
		return hoursSqlQueryString;
	}

	public List<Integer> getHoursIntList() {
		return hoursIntList;
	}
}
