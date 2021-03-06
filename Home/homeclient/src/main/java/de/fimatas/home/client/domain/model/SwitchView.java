package de.fimatas.home.client.domain.model;

public class SwitchView extends View {

    private String label = "";

    private String link = "#";

    private String linkOn = "#";

    private String linkOff = "#";

    private String linkAuto = "#";

    private String linkManual = "#";

    private String autoInfoText = "";

    private String buttonCaptionAuto = "";

    private String buttonCaptionManual = "";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getButtonCaptionAuto() {
        return buttonCaptionAuto;
    }

    public void setButtonCaptionAuto(String buttonCaptionAuto) {
        this.buttonCaptionAuto = buttonCaptionAuto;
    }

    public String getButtonCaptionManual() {
        return buttonCaptionManual;
    }

    public void setButtonCaptionManual(String buttonCaptionManual) {
        this.buttonCaptionManual = buttonCaptionManual;
    }

}
