package home.domain.model;

import java.util.LinkedList;
import java.util.List;

public class Pages {

	public static final String PATH_HOME = "/";

	public static final String PATH_HISTORY = "/history"; // NOSONAR

	public static final String PATH_LINKS = "/links"; // NOSONAR

	public static final String PATH_SETTINGS = "/settings"; // NOSONAR

	public static final String PATH_LOGOFF = "/logoff"; // NOSONAR

	private static final List<PageEntry> ENTRIES = new LinkedList<>();

	static {
		ENTRIES.add(new PageEntry("Zuhause", PATH_HOME, "fas fa-home", "home"));
		ENTRIES.add(new PageEntry("Historie", PATH_HISTORY, "fas fa-history", "history"));
		ENTRIES.add(new PageEntry("Links", PATH_LINKS, "fas fa-link", "links"));
		ENTRIES.add(new PageEntry("Einstellungen", PATH_SETTINGS, "fas fa-sliders-h", "settings"));
		ENTRIES.add(new PageEntry("abmelden", PATH_LOGOFF, "", ""));
	}

	private Pages() {
		super();
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
