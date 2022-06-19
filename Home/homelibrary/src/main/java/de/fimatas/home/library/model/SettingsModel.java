package de.fimatas.home.library.model;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import de.fimatas.home.library.domain.model.PushNotifications;
import lombok.Data;

@Data
public class SettingsModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public SettingsModel() {
        super();
    }

    private String token;

    private String user;

    private String client;

    private long lastTimestamp;

    private EnumMap<PushNotifications, Boolean> pushNotifications = new EnumMap<>(PushNotifications.class);

    private Map<String, String> attributes = new HashMap<>();


}
