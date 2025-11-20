package de.fimatas.home.library.model;

import lombok.Getter;

@Getter
public enum MessageType {

    REFRESH_ALL_MODELS(null), //
    TOGGLESTATE(Pages.PATH_HOME), //
    TOGGLELIGHT(Pages.PATH_HOME), //
    FRONTDOOR(Pages.PATH_HOME), //
    TOGGLEAUTOMATION(Pages.PATH_HOME), //
    SHUTTERPOSITION(Pages.PATH_HOME), //
    HEATINGBOOST(Pages.PATH_HOME), //
    HEATINGMANUAL(Pages.PATH_HOME), //
    HEATINGAUTO(Pages.PATH_HOME), //
    SLIDERVALUE(Pages.PATH_HOME), //
    CHARGELIMIT(Pages.PATH_HOME), //
    CONTROL_HEATPUMP_ROOF(Pages.PATH_HOME), //
    CONTROL_HEATPUMP_BASEMENT(Pages.PATH_HOME), //
    SETTINGS_NEW(null), //
    SETTINGS_EDIT(null), //
    WALLBOX_SELECTED_EV(Pages.PATH_HOME),
    PRESENCE_EDIT(null), //
    LIVEACTIVITY_START(null), //
    LIVEACTIVITY_END(null), //
    PV_OVERFLOW_MAX_WATTS_GRID(Pages.PATH_HOME), //
    PV_OVERFLOW_MIN_PATTERY_PERCENTAGE(Pages.PATH_HOME), //
    TASKS_EXECUTION(Pages.PATH_HOME), //
    MAINTENANCE(Pages.PATH_MAINTENANCE), //
    ;//

    private final String targetSite;

    MessageType(String targetSite) {
        this.targetSite = targetSite;
    }

}
