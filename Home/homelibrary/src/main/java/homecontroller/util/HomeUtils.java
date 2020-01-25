package homecontroller.util;

import java.time.LocalDateTime;

import org.springframework.util.StringUtils;

public class HomeUtils {

	private HomeUtils() {
		super();
	}

	public static String escape(String string) {
		string = StringUtils.replace(string, " ", "");
		string = StringUtils.replace(string, ".", "_");
		string = StringUtils.replace(string, "-", "_");
		string = StringUtils.replace(string, ":", "_");
		string = StringUtils.replace(string, "ä", "ae");
		string = StringUtils.replace(string, "ö", "oe");
		string = StringUtils.replace(string, "ü", "ue");
		string = StringUtils.replace(string, "Ä", "Ae");
		string = StringUtils.replace(string, "Ö", "Oe");
		string = StringUtils.replace(string, "Ü", "Ue");
		string = StringUtils.replace(string, "ß", "ss");
		return string;
	}

	public static boolean isSameMonth(LocalDateTime date1, LocalDateTime date2) {
		return date1.getYear() == date2.getYear() && date1.getMonthValue() == date2.getMonthValue();
	}

	public static boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
		return isSameMonth(date1, date2) && date1.getDayOfMonth() == date2.getDayOfMonth();
	}
}
