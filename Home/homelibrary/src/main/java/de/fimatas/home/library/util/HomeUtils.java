package de.fimatas.home.library.util;

import de.fimatas.home.library.domain.model.Tendency;
import de.fimatas.home.library.domain.model.ValueWithTendency;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

public class HomeUtils {

    private HomeUtils() {
        super();
    }

    public static String escape(String string) {

        if (string == null) {
            return null;
        }
        string = string.replace(" ", "");
        string = string.replace(".", "_");
        string = string.replace("-", "_");
        string = string.replace(":", "_");
        string = string.replace("ä", "ae");
        string = string.replace("ö", "oe");
        string = string.replace("ü", "ue");
        string = string.replace("Ä", "Ae");
        string = string.replace("Ö", "Oe");
        string = string.replace("Ü", "Ue");
        string = string.replace("ß", "ss");
        return string;
    }

    public static boolean isSameMonth(LocalDateTime date1, LocalDateTime date2) {
        return date1.getYear() == date2.getYear() && date1.getMonthValue() == date2.getMonthValue();
    }

    public static boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
        return isSameMonth(date1, date2) && date1.getDayOfMonth() == date2.getDayOfMonth();
    }

    public static DecimalFormat buildDecimalFormat(String format){
        return new DecimalFormat(format, new DecimalFormatSymbols(Locale.GERMAN));
    }

    public static Integer roundPercentageToNearestTen(Integer percentage) {
        if (percentage == null) {
            return null;
        }
        return (percentage / 10 * 10) + (percentage % 10 < 5 ? 0 : 10);
    }

    public static String roundAndFormatPrecipitation(BigDecimal precipitation) {
        if(precipitation.compareTo(BigDecimal.ZERO) == 0){
            return "";
        }
        BigDecimal rounded = precipitation.setScale(0, RoundingMode.HALF_UP);
        if(rounded.compareTo(BigDecimal.ZERO) == 0){
            return "<1mm";
        }
        return rounded + "mm";
    }

    public static String durationSinceFormatted(Instant instant, boolean padded, boolean useNow, boolean useAgo){
        var duration = Duration.between(instant, Instant.now());
        var prefix = useAgo ? "vor " : "";
        if(duration.toHours() >= 5){
            if(duration.toDays() >= 5){
                var days = duration.toDays();
                return prefix + StringUtils.leftPad(Long.toString(days), padded ? 5 : 0) + " Tage" + (useAgo ? "n" : "");
            }else{
                var hours = duration.toHours();
                return prefix + StringUtils.leftPad(Long.toString(hours), padded ? 5 : 0) + " Stunden";
            }
        }else {
            var minutes = duration.toMinutes();
            if(minutes == 0 && useNow){
                return "jetzt";
            }
            return prefix + StringUtils.leftPad(Long.toString(minutes), padded ? 5 : 0) + " Minute" + (minutes == 1 ? "" : "n");
        }
    }

    public static void calculateTendency(long newTimestamp, ValueWithTendency<BigDecimal> reference,
                                   ValueWithTendency<BigDecimal> actual, BigDecimal diffValue) {

        if (actual.getValue() == null) {
            actual.setTendency(Tendency.NONE);
            return;
        }

        if (reference == null || reference.getReferenceValue() == null) {
            actual.setTendency(Tendency.NONE);
            actual.setReferenceValue(actual.getValue());
            return;
        }

        BigDecimal diff = actual.getValue().subtract(reference.getReferenceValue());

        if (diff.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(diffValue) > 0) {
            actual.setTendency(Tendency.RISE);
            actual.setReferenceValue(actual.getValue());
            actual.setReferenceDateTime(newTimestamp);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0 && diff.abs().compareTo(diffValue) > 0) {
            actual.setTendency(Tendency.FALL);
            actual.setReferenceValue(actual.getValue());
            actual.setReferenceDateTime(newTimestamp);
        } else {
            long timeDiff = newTimestamp - reference.getReferenceDateTime();
            actual.setTendency(Tendency.calculate(reference, timeDiff));
            actual.setReferenceValue(reference.getReferenceValue());
            actual.setReferenceDateTime(reference.getReferenceDateTime());
        }
    }
}
