package de.fimatas.home.library.domain.model;

public enum Setting {

    DOORBELL("Türklingel", true), //
    ;

    private final String text;

    private final boolean defaultValue;

    private Setting(String text, boolean defaultValue) {
        this.text = text;
        this.defaultValue = defaultValue;
    }

    public String getText() {
        return text;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

}
