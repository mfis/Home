package de.fimatas.home.library.util;

import java.time.LocalDateTime;

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
}
