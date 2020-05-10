package de.fimatas.home.client.domain.model;

public class LockView extends View {

    private String caption = "";

    private String linkAuto = "#";

    private String linkAutoEvent = "#";

    private String linkManual = "#";

    private String autoInfoText = "";

    private String linkLock = "#";

    private String linkUnlock = "#";

    private String linkOpen = "#";

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

    public String getLinkLock() {
        return linkLock;
    }

    public void setLinkLock(String linkLock) {
        this.linkLock = linkLock;
    }

    public String getLinkUnlock() {
        return linkUnlock;
    }

    public void setLinkUnlock(String linkUnlock) {
        this.linkUnlock = linkUnlock;
    }

    public String getLinkOpen() {
        return linkOpen;
    }

    public void setLinkOpen(String linkOpen) {
        this.linkOpen = linkOpen;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getLinkAutoEvent() {
        return linkAutoEvent;
    }

    public void setLinkAutoEvent(String linkAutoEvent) {
        this.linkAutoEvent = linkAutoEvent;
    }

}
