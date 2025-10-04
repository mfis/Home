package de.fimatas.home.library.model;

public enum ConditionColor {

    RED("danger"),
    ORANGE("warning"),
    COLD("cold"),
    BLUE("info"),
    GREEN("success"),
    LIGHT("light"),
    DEFAULT("default"),
    GRAY("secondary"),
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
