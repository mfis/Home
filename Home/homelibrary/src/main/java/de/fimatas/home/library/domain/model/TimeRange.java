package de.fimatas.home.library.domain.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TimeRange {

    NIGHT(Arrays.asList(0, 1, 2, 3, 4, 5)), //

    MORGING(Arrays.asList(6, 7, 8, 9, 10, 11)), //

    DAY(Arrays.asList(12, 13, 14, 15, 16, 17)), //

    EVENING(Arrays.asList(18, 19, 20, 21, 22, 23)), //
    ;

    private final List<Integer> hoursIntList;

    TimeRange(List<Integer> hoursIntList) {
        this.hoursIntList = hoursIntList;
    }

    public static TimeRange fromDateTime(LocalDateTime localDateTime) {
        int hours = localDateTime.getHour();
        for (TimeRange range : values()) {
            if (range.hoursIntList.contains(hours)) {
                return range;
            }
        }
        throw new IllegalStateException("fromDateTime: unexpected hour: " + hours);
    }

    public static String hoursSqlQueryString(List<TimeRange> ranges) {
        return "in (" +
                ranges.stream().map(tr -> tr.hoursIntList).
                flatMap(List::stream).map(Object::toString).collect(Collectors.joining(","))
                + ")";
    }

    public static List<Integer> hoursIntList(List<TimeRange> ranges) {
        return ranges.stream().map(tr -> tr.hoursIntList).
                flatMap(List::stream).collect(Collectors.toList());
    }
}
