package de.fimatas.home.library.model;

public enum MessageType {

    REFRESH_ALL_MODELS(null), //
    TOGGLESTATE(Pages.PATH_HOME), //
    TOGGLELIGHT(Pages.PATH_HOME), //
    OPEN(Pages.PATH_HOME), //
    TOGGLEAUTOMATION(Pages.PATH_HOME), //
    SHUTTERPOSITION(Pages.PATH_HOME), //
    HEATINGBOOST(Pages.PATH_HOME), //
    HEATINGMANUAL(Pages.PATH_HOME), //
    HEATINGAUTO(Pages.PATH_HOME), //
    SLIDERVALUE(Pages.PATH_HOME), //
    CHARGELIMIT(Pages.PATH_HOME), //
    CONTROL_HEATPUMP(Pages.PATH_HOME), //
    SETTINGS_NEW(null), //
    SETTINGS_EDIT(null), //
    WALLBOX_SELECTED_EV(Pages.PATH_HOME),
    PRESENCE_EDIT(null), //
    LIVEACTIVITY_START(null), //
    LIVEACTIVITY_END(null), //
    PV_OVERFLOW_MAX_WATTS_GRID(Pages.PATH_HOME), //
    ;//

    private final String targetSite;

    private MessageType(String targetSite) {
        this.targetSite = targetSite;
    }

    public String getTargetSite() {
        return targetSite;
    }
}
