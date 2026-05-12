package de.fimatas.home.library.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PageEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    private String path;

    private String icon;

    private String template;

    public PageEntry(String name, String path, String icon, String template) {
        this.name = name;
        this.path = path;
        this.icon = icon;
        this.template = template;
    }
}
