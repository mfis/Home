package de.fimatas.home.library.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class SettingsContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<SettingsModel> settings = new LinkedList<>();

    public List<SettingsModel> getSettings() {
        return settings;
    }

    public void setSettings(List<SettingsModel> settings) {
        this.settings = settings;
    }
}
