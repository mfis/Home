package de.fimatas.home.client.domain.model;

public class ShutterView extends View {

    private String linkOpen = "#";

    private String linkHalf = "#";

    private String linkSunshade = "#";

    private String linkClose = "#";

    private String linkAuto = "#";

    private String linkManual = "#";

    private String autoInfoText = "";

    private String iconOpen = "";

    private String iconHalf = "";

    private String iconSunshade = "";

    private String iconClose = "";

    public String getLinkOpen() {
        return linkOpen;
    }

    public void setLinkOpen(String linkOpen) {
        this.linkOpen = linkOpen;
    }

    public String getLinkSunshade() {
        return linkSunshade;
    }

    public void setLinkSunshade(String linkSunshade) {
        this.linkSunshade = linkSunshade;
    }

    public String getLinkClose() {
        return linkClose;
    }

    public void setLinkClose(String linkClose) {
        this.linkClose = linkClose;
    }

    public String getLinkAuto() {
        return linkAuto;
    }

    public void setLinkAuto(String linkAuto) {
        this.linkAuto = linkAuto;
    }

    public String getLinkManual() {
        return linkManual;
    }

    public void setLinkManual(String linkManual) {
        this.linkManual = linkManual;
    }

    public String getAutoInfoText() {
        return autoInfoText;
    }

    public void setAutoInfoText(String autoInfoText) {
        this.autoInfoText = autoInfoText;
    }

    public String getIconOpen() {
        return iconOpen;
    }

    public void setIconOpen(String iconOpen) {
        this.iconOpen = iconOpen;
    }

    public String getIconSunshade() {
        return iconSunshade;
    }

    public void setIconSunshade(String iconSunshade) {
        this.iconSunshade = iconSunshade;
    }

    public String getIconClose() {
        return iconClose;
    }

    public void setIconClose(String iconClose) {
        this.iconClose = iconClose;
    }

    public String getLinkHalf() {
        return linkHalf;
    }

    public void setLinkHalf(String linkHalf) {
        this.linkHalf = linkHalf;
    }

    public String getIconHalf() {
        return iconHalf;
    }

    public void setIconHalf(String iconHalf) {
        this.iconHalf = iconHalf;
    }
}
