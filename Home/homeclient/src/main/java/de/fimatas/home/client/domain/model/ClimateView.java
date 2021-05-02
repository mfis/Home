package de.fimatas.home.client.domain.model;

import java.util.LinkedList;
import java.util.List;

public class ClimateView extends View {

    private String postfix = "";

    private String stateTemperature = "";

    private String statePostfixIconTemperature = "";

    private String tendencyIconTemperature = "";

    private String stateHumidity = "";

    private String stateSecondLine = "";

    private String statePostfixIconHumidity = "";

    private String tendencyIconHumidity = "";

    private String colorClassHumidity = "secondary";

    private String colorClassHeating = "secondary";

    private String linkBoost = "";

    private String linkManual = "";

    private String linkAdjustTemperature = "";

    private String linkAuto = "";

    private String targetTemp = "";

    private String boostTimeLeft = "";

    private String heatericon = "";

    private String heaterElementTitleState = "";

    private String absoluteHumidityIcon = "";

    private List<String> hints = new LinkedList<>();

    public String getLinkBoost() {
        return linkBoost;
    }

    public void setLinkBoost(String linkBoost) {
        this.linkBoost = linkBoost;
    }

    public String getLinkManual() {
        return linkManual;
    }

    public void setLinkManual(String linkManual) {
        this.linkManual = linkManual;
    }

    public String getTargetTemp() {
        return targetTemp;
    }

    public void setTargetTemp(String targetTemp) {
        this.targetTemp = targetTemp;
    }

    public String getHeatericon() {
        return heatericon;
    }

    public void setHeatericon(String heatericon) {
        this.heatericon = heatericon;
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
    }

    public List<String> getHints() {
        return hints;
    }

    public void setHints(List<String> hints) {
        this.hints = hints;
    }

    public String getStateTemperature() {
        return stateTemperature;
    }

    public void setStateTemperature(String stateTemperature) {
        this.stateTemperature = stateTemperature;
    }

    public String getStatePostfixIconTemperature() {
        return statePostfixIconTemperature;
    }

    public void setStatePostfixIconTemperature(String statePostfixIconTemperature) {
        this.statePostfixIconTemperature = statePostfixIconTemperature;
    }

    public String getTendencyIconTemperature() {
        return tendencyIconTemperature;
    }

    public void setTendencyIconTemperature(String tendencyIconTemperature) {
        this.tendencyIconTemperature = tendencyIconTemperature;
    }

    public String getStateHumidity() {
        return stateHumidity;
    }

    public void setStateHumidity(String stateHumidity) {
        this.stateHumidity = stateHumidity;
    }

    public String getStatePostfixIconHumidity() {
        return statePostfixIconHumidity;
    }

    public void setStatePostfixIconHumidity(String statePostfixIconHumidity) {
        this.statePostfixIconHumidity = statePostfixIconHumidity;
    }

    public String getTendencyIconHumidity() {
        return tendencyIconHumidity;
    }

    public void setTendencyIconHumidity(String tendencyIconHumidity) {
        this.tendencyIconHumidity = tendencyIconHumidity;
    }

    public String getColorClassHeating() {
        return colorClassHeating;
    }

    public void setColorClassHeating(String colorClassHeating) {
        this.colorClassHeating = colorClassHeating;
    }

    public String getAbsoluteHumidityIcon() {
        return absoluteHumidityIcon;
    }

    public void setAbsoluteHumidityIcon(String absoluteHumidityIcon) {
        this.absoluteHumidityIcon = absoluteHumidityIcon;
    }

    public String getColorClassHumidity() {
        return colorClassHumidity;
    }

    public void setColorClassHumidity(String colorClassHumidity) {
        this.colorClassHumidity = colorClassHumidity;
    }

    public String getHeaterElementTitleState() {
        return heaterElementTitleState;
    }

    public void setHeaterElementTitleState(String heaterElementTitleState) {
        this.heaterElementTitleState = heaterElementTitleState;
    }

    public String getStateSecondLine() {
        return stateSecondLine;
    }

    public void setStateSecondLine(String stateSecondLine) {
        this.stateSecondLine = stateSecondLine;
    }

    public String getLinkAuto() {
        return linkAuto;
    }

    public void setLinkAuto(String linkAuto) {
        this.linkAuto = linkAuto;
    }

    public String getBoostTimeLeft() {
        return boostTimeLeft;
    }

    public void setBoostTimeLeft(String boostTimeLeft) {
        this.boostTimeLeft = boostTimeLeft;
    }

    public String getLinkAdjustTemperature() {
        return linkAdjustTemperature;
    }

    public void setLinkAdjustTemperature(String linkAdjustTemperature) {
        this.linkAdjustTemperature = linkAdjustTemperature;
    }
}
