package de.fimatas.home.library.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;

import static java.lang.Math.round;

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
            return "< 1 mm";
        }
        return rounded + " mm";
    }
}
