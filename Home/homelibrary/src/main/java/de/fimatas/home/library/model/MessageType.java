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

    CONTROL_HEATPUMP(Pages.PATH_HOME), //
    CAMERAPICTUREREQUEST(null), //
    SETTINGS_NEW(null), //
    SETTINGS_EDIT(null), //

    PRESENCE_EDIT(null), //
    ;//

    private final String targetSite;

    private MessageType(String targetSite) {
        this.targetSite = targetSite;
    }

    public String getTargetSite() {
        return targetSite;
    }
}
