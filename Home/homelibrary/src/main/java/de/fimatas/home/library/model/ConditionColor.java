package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.Place;

public enum ConditionColor {

    DEFAULT("default"),
    RED("danger"),
    ORANGE("warning"),
    GREEN("success"),
    BLUE("info"),
    LIGHT("light"),
    GRAY("secondary"),
    COLD("cold"),
    ACTIVE_BUTTON("active-primary"),

    ROW_STRIPE_DEFAULT("default"),

    ROW_STRIPE_ACCENT("accent"),
    ;

    private final String uiClass;

    ConditionColor(String uiClass){
        this.uiClass = uiClass;
    }

    public static ConditionColor fromUiName(String uiname) {
        for (ConditionColor color : values()) {
            if (color.getUiClass().equalsIgnoreCase(uiname)) {
                return color;
            }
        }
        return null;
    }

    public final String getUiClass() {
        return uiClass;
    }
}
