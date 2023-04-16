package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;

import java.util.List;
import java.util.Map;

import static de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter.*;

public enum WeatherConditions {

    GUST("fa-solid fa-wind",  "wind","Böen", true, ConditionColor.RED),
    WIND("fa-solid fa-wind",  "wind","Wind", true, ConditionColor.ORANGE),
    SNOW("fa-solid fa-snowflake", "snowflake", "Schnee", true, ConditionColor.COLD),
    HAIL("fa-solid fa-cloud-meatball", "cloud.hail", "Hagel", true, null),
    THUNDERSTORM("fa-solid fa-cloud-bolt", "bolt", "Gewitter", true, null),
    RAIN("fa-solid fa-cloud-showers-heavy", "cloud.heavyrain", "Regen", true, null),
    CLOUD_RAIN("fa-solid fa-cloud-rain", "cloud.rain", "Leichter Regen", true, null),
    SUN("fa-solid fa-sun", "sun.max", "Sonne", true, null),
    FOG("fa-solid fa-smog", "cloud.fog", "Nebel", false, null),
    SUN_CLOUD("fa-solid fa-cloud-sun", "cloud.sun", "Leicht bewölkt", true, null),
    CLOUD("fa-solid fa-cloud", "cloud.fill", "Wolken", false, null),
    MOON("fa-solid fa-moon", "moon.fill", "Mond", false, null),
    MOON_CLOUD("fa-solid fa-cloud-moon", "cloud.moon", "Leicht bewölkt", false, null),
    UNKNOWN("fa-solid fa-circle-question", "", "Unbekannt", false, null)
    ;

    private final String fontAwesomeID;
    private final String sfSymbolsID;
    private final String caption;
    private final boolean significant;
    private final ConditionColor color;

    WeatherConditions(String fontAwesomeID, String sfSymbolsID, String caption, boolean significant, ConditionColor color){
        this.fontAwesomeID = fontAwesomeID;
        this.sfSymbolsID = sfSymbolsID;
        this.caption = caption;
        this.significant = significant;
        this.color = color;
    }

    public Integer ordinalAsInteger(){
        return (Integer) ordinal();
    }

    public String conditionValue(Map<Integer, String> textMap){
        switch (this){
            case WIND:
            case GUST:
                return textMap.get(WIND_GUST_TEXT);
            case SNOW:
            case THUNDERSTORM:
            case RAIN:
            case CLOUD_RAIN:
                return textMap.get(PRECIPATION_TEXT);
            case SUN:
            case SUN_CLOUD:
                return textMap.get(SUNDURATION_TEXT);
            default:
                return "";
        }
    }

    public static List<WeatherConditions> lessSignificantConditions(){
        return List.of(SUN_CLOUD, CLOUD);
    }

    public String getFontAwesomeID() {
        return fontAwesomeID;
    }

    public String getSfSymbolsID() {
        return sfSymbolsID;
    }

    public String getCaption() {
        return caption;
    }

    public boolean isSignificant(){return significant;}

    public ConditionColor getColor() {
        return color;
    }

    public boolean isKindOfRain(){
        return this == RAIN || this == CLOUD_RAIN || this == HAIL || this == THUNDERSTORM;
    }
}
