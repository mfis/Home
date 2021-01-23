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

    private EnumMap<PushNotifications, Boolean> pushNotifications = new EnumMap<>(PushNotifications.class);

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

    public EnumMap<PushNotifications, Boolean> getPushNotifications() { // NOSONAR
        return pushNotifications;
    }

    public void setPushNotifications(EnumMap<PushNotifications, Boolean> pushNotifications) { // NOSONAR
        this.pushNotifications = pushNotifications;
    }

}
