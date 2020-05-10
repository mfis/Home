package de.fimatas.home.library.model;

import java.io.Serializable;

public class PageEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String path;

    private final String icon;

    private final String template;

    public PageEntry(String name, String path, String icon, String template) {
        this.name = name;
        this.path = path;
        this.icon = icon;
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getIcon() {
        return icon;
    }

    public String getTemplate() {
        return template;
    }

}
