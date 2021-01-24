package de.fimatas.home.library.domain.model;

public enum PushNotifications {

    DOORBELL("Türklingelbetätigung:", true), //
    WINDOW_OPEN("Fenster noch geöffnet:", true), //
    ;

    private final String text;

    private final boolean defaultSetting;

    private PushNotifications(String text, boolean defaultSetting) {
        this.text = text;
        this.defaultSetting = defaultSetting;
    }

    public String getText() {
        return text;
    }

    public boolean getDefaultSetting() {
        return defaultSetting;
    }

}
