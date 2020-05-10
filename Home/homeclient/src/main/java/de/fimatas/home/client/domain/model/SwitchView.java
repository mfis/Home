package de.fimatas.home.client.domain.model;

public class SwitchView extends View {

    private String label = "";

    private String link = "#";

    private String linkAuto = "#";

    private String linkManual = "#";

    private String autoInfoText = "";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
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

}
