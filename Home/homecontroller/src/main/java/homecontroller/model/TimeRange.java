package homecontroller.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public enum TimeRange {

	DAY("in (9,10,11,12,13,14,15,16,17,18,19,20,21,22,23)", //
			Arrays.asList(9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)), //

	NIGHT("in (0,1,2,3,4,5,6,7,8)", //
			Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8)), //
	;

	private final String hoursSqlQueryString;

	private final List<Integer> hoursIntList;

	private TimeRange(String hoursSqlQueryString, List<Integer> hoursIntList) {
		this.hoursSqlQueryString = hoursSqlQueryString;
		this.hoursIntList = hoursIntList;
	}

	public static TimeRange fromDateTime(LocalDateTime localDateTime) {
		int hours = localDateTime.getHour();
		if (DAY.getHoursIntList().contains(hours)) {
			return DAY;
		} else if (NIGHT.getHoursIntList().contains(hours)) {
			return NIGHT;
		} else {
			throw new IllegalStateException("fromDateTime: unexpected hour: " + hours);
		}
	}

	public String getHoursSqlQueryString() {
		return hoursSqlQueryString;
	}

	public List<Integer> getHoursIntList() {
		return hoursIntList;
	}
}
