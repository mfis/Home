package de.fimatas.home.library.model;

import java.util.LinkedList;
import java.util.List;

public class Pages {

    public static final String PATH_HOME = "/";

    public static final String PATH_APP = "/appInstallation";

    public static final String PATH_REPAIR = "/repair";

    public static final String PATH_LOGOFF = "/logoff"; // NOSONAR

    private static final List<PageEntry> ENTRIES = new LinkedList<>();

    static {
        ENTRIES.add(new PageEntry("Zuhause", PATH_HOME, "fas fa-home", "home"));
        ENTRIES.add(new PageEntry("iOS App", PATH_APP, "fab fa-app-store-ios", "appInstallation"));
        ENTRIES.add(new PageEntry("Reparatur", PATH_REPAIR, "fa-solid fa-screwdriver-wrench", "repair"));
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
        return null;
    }

    public static PageEntry getAppHomeEntry() {
        return new PageEntry("", PATH_HOME, "fas fa-home", "home");
    }

}
