package de.fimatas.home.library.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TimeRange {

    NIGHT(Arrays.asList(0, 1, 2, 3, 4, 5), "Nacht"), //

    MORGING(Arrays.asList(6, 7, 8, 9, 10, 11), "Vormittag"), //

    DAY(Arrays.asList(12, 13, 14, 15, 16, 17), "Nachmittag"), //

    EVENING(Arrays.asList(18, 19, 20, 21, 22, 23), "Abend"), //
    ;

    private final List<Integer> hoursIntList;

    @Getter
    private final String label;

    TimeRange(List<Integer> hoursIntList, String label) {
        this.hoursIntList = hoursIntList;
        this.label = label;
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

    public String hoursFormToLabel(){
        return hoursIntList.get(0) + ".." + hoursIntList.get(hoursIntList.size() - 1) + " Uhr";
    }
}
