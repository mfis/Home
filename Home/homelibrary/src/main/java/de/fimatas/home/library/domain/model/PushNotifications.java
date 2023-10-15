package de.fimatas.home.library.domain.model;

public enum PushNotifications {

    DOORBELL("Haustür", "Türklingel",true), //
    WINDOW_OPEN("Fenster noch geöffnet", "Geöffnete Fenster",true), //
    LOW_BATTERY("Batterie fast leer", "Leere Batterien",true), //
    WEATHER_TODAY("Das Wetter heute", "Tageswetter",false), //
    CHARGELIMIT_OK("Ladevorgang abgeschlossen", "Ladevorgang OK",false), //
    CHARGELIMIT_ERROR("Ladevorgang unterbrochen", "Ladevorgang Fehler",false), //
    ERRORMESSAGE("Fehlermeldung", "Fehlermeldungen",false), //
    NOTICE("Hinweise", "Hinweise",false), //
    ;

    private final String pushText;

    private final String settingsText;

    private final boolean defaultSetting;

    PushNotifications(String pushText, String settingsText, boolean defaultSetting) {
        this.pushText = pushText;
        this.settingsText = settingsText;
        this.defaultSetting = defaultSetting;
    }

    public String getPushText() {
        return pushText;
    }

    public String getSettingsText() {
        return settingsText;
    }

    public boolean getDefaultSetting() {
        return defaultSetting;
    }

}
