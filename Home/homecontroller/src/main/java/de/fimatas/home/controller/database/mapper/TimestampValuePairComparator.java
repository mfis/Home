package de.fimatas.home.controller.database.mapper;

import java.util.Comparator;

public class TimestampValuePairComparator implements Comparator<TimestampValuePair> {

    @Override
    public int compare(TimestampValuePair a, TimestampValuePair b) {

        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.getTimestamp().compareTo(b.getTimestamp());
    }
}
