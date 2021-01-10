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
    CAMERAPICTUREREQUEST(null), //
    SETTINGS_CLIENTNAME(Pages.PATH_SETTINGS), //
    SETTINGS_PUSH_HINTS(Pages.PATH_SETTINGS), //
    SETTINGS_PUSH_HINTS_HYSTERESIS(Pages.PATH_SETTINGS), //
    SETTINGS_PUSH_DOORBELL(Pages.PATH_SETTINGS), //
    ;//

    private final String targetSite;

    private MessageType(String targetSite) {
        this.targetSite = targetSite;
    }

    public String getTargetSite() {
        return targetSite;
    }
}
