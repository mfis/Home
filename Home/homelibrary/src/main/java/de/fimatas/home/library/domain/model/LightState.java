package de.fimatas.home.library.domain.model;

public enum LightState {

    ON("Ein"), OFF("Aus"), SWITCH_OFF("Nicht erreichbar")
    ;

    private final String caption;

    private LightState(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }

}
