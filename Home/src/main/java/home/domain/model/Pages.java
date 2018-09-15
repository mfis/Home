package home.domain.model;

import java.util.LinkedList;
import java.util.List;

public class Pages {

	public final static String PATH_HOME = "/";

	public final static String PATH_HISTORY = "/history";

	public final static String PATH_LINKS = "/links";

	public final static String PATH_LOGOFF = "/logoff";

	private final static List<PageEntry> ENTRIES = new LinkedList<>();

	static {
		ENTRIES.add(new PageEntry("Zuhause", PATH_HOME, "fas fa-home", "home"));
		ENTRIES.add(new PageEntry("Historie", PATH_HISTORY, "fas fa-history", "history"));
		ENTRIES.add(new PageEntry("Links", PATH_LINKS, "fas fa-link", "links"));
		ENTRIES.add(new PageEntry("abmelden", PATH_LOGOFF, "", ""));
	}

	public static List<PageEntry> getOtherEntries(String path) {
		List<PageEntry> list = new LinkedList<>();
		for (PageEntry entry : ENTRIES) {
			if (!entry.getPath().equals(path)) {
				list.add(entry);
			}
		}
		return list;
	}

	public static PageEntry getEntry(String path) {
		for (PageEntry entry : ENTRIES) {
			if (entry.getPath().equals(path)) {
				return entry;
			}
		}
		throw new NullPointerException("Entry not found:" + path);
	}

}
