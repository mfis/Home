package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.util.EnumMap;

public class SettingsModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public SettingsModel() {
        super();
    }

    private String token;

    private String user;

    private long lastTimestamp;

    private EnumMap<Setting, Boolean> settings = new EnumMap<>(Setting.class);

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public EnumMap<Setting, Boolean> getSettings() { // NOSONAR
        return settings;
    }

    public void setSettings(EnumMap<Setting, Boolean> settings) { // NOSONAR
        this.settings = settings;
    }

}
