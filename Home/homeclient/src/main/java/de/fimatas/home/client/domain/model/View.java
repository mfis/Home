package de.fimatas.home.client.domain.model;

import de.fimatas.home.library.domain.model.Place;

public class View { private String id = "";

    private String state = "";

    private String stateShort = ""; // watch

    private String elementTitleState = "";

    private String stateSuffix = "";

    private String icon = "";

    private String iconNativeClient = "";

    private String name = "";

    private String shortName = "";

    private String place = "";

    private String placeID = "";

    private String historyKey = "";

    private String busy = "";

    private String unreach = "";

    private String colorClass = "default";

    private String activeSwitchColorClass = "light";

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconNativeClient() {
        return iconNativeClient;
    }

    public void setIconNativeClient(String iconNativeClient) {
        this.iconNativeClient = iconNativeClient;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHistoryKey() {
        return historyKey;
    }

    public void setHistoryKey(String historyKey) {
        this.historyKey = historyKey;
    }

    public String getPlace() {
        return place;
    }

    public void setPlaceEnum(Place p) {
        this.place = p.getPlaceName();
        this.placeID = p.name();
    }

    public String getPlaceID() {
        return placeID;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStateSuffix() {
        return stateSuffix;
    }

    public void setStateSuffix(String stateSuffix) {
        this.stateSuffix = stateSuffix;
    }

    public String getBusy() {
        return busy;
    }

    public void setBusy(String busy) {
        this.busy = busy;
    }

    public String getUnreach() {
        return unreach;
    }

    public void setUnreach(String unreach) {
        this.unreach = unreach;
    }

    public String getColorClass() {
        return colorClass;
    }

    public void setColorClass(String colorClass) {
        this.colorClass = colorClass;
    }

    public String getStateShort() {
        return stateShort;
    }

    public void setStateShort(String stateShort) {
        this.stateShort = stateShort;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getActiveSwitchColorClass() {
        return activeSwitchColorClass;
    }

    public void setActiveSwitchColorClass(String activeSwitchColorClass) {
        this.activeSwitchColorClass = activeSwitchColorClass;
    }

    public String getElementTitleState() {
        return elementTitleState;
    }

    public void setElementTitleState(String elementTitleState) {
        this.elementTitleState = elementTitleState;
    }

}
