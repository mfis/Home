package de.fimatas.home.client.domain.model;

public class LightView extends View {

    private String linkOn = "#";

    private String linkOff = "#";

    public String getLinkOn() {
        return linkOn;
    }

    public void setLinkOn(String linkOn) {
        this.linkOn = linkOn;
    }

    public String getLinkOff() {
        return linkOff;
    }

    public void setLinkOff(String linkOff) {
        this.linkOff = linkOff;
    }
}
