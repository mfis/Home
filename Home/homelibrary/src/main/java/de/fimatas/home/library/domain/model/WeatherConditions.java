package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

import static de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter.*;

public enum WeatherConditions {

    GUST("fa-solid fa-wind",  "wind","Böen", 1, ConditionColor.RED),
    WIND("fa-solid fa-wind",  "wind","Wind", 1, ConditionColor.ORANGE),
    SNOW("fa-solid fa-snowflake", "snowflake", "Schnee", 1, ConditionColor.COLD),
    HAIL("fa-solid fa-cloud-meatball", "cloud.hail", "Hagel", 1, null),
    THUNDERSTORM("fa-solid fa-cloud-bolt", "bolt", "Gewitter", 1, null),
    HEAVY_RAIN("fa-solid fa-cloud-showers-heavy", "cloud.heavyrain", "Starkregen", 1, ConditionColor.RED),
    RAIN("fa-solid fa-cloud-rain", "cloud.rain", "Regen", 1, null),
    SUN("fa-solid fa-sun", "sun.max", "Sonne", 1, null),
    FOG("fa-solid fa-smog", "cloud.fog", "Nebel", 1, null),
    SUN_CLOUD("fa-solid fa-cloud-sun", "cloud.sun", "Leicht bewölkt", 1, null),
    CLOUD("fa-solid fa-cloud", "cloud.fill", "Wolken", 2, null),
    MOON("fa-solid fa-moon", "moon.fill", "Mond", 0, null),
    MOON_CLOUD("fa-solid fa-cloud-moon", "cloud.moon", "Leicht bewölkt", 0, null),
    UNKNOWN("fa-solid fa-circle-question", "", "Unbekannt", 0, null)
    ;

    @Getter
    private final String fontAwesomeID;
    @Getter
    private final String sfSymbolsID;
    @Getter
    private final String caption;
    private final int significant;
    @Getter
    private final ConditionColor color;

    WeatherConditions(String fontAwesomeID, String sfSymbolsID, String caption, int significant, ConditionColor color){
        this.fontAwesomeID = fontAwesomeID;
        this.sfSymbolsID = sfSymbolsID;
        this.caption = caption;
        this.significant = significant;
        this.color = color;
    }

    public Integer ordinalAsInteger(){
        return ordinal();
    }

    public String conditionValue(Map<Integer, String> textMap){
        return switch (this) {
            case WIND, GUST -> textMap.get(WIND_GUST_TEXT);
            case SNOW, THUNDERSTORM, HEAVY_RAIN, RAIN -> textMap.get(PRECIPATION_TEXT);
            case SUN, SUN_CLOUD -> textMap.get(SUNDURATION_TEXT);
            default -> "";
        };
    }

    public static List<WeatherConditions> lessSignificantConditions(){
        return List.of(SUN_CLOUD, CLOUD);
    }

    public boolean isSignificant(){return significant == 1;}

    public boolean isLessSignificant(){return significant == 2;}

    public boolean isKindOfRain(){
        return this == HEAVY_RAIN || this == RAIN || this == HAIL || this == THUNDERSTORM;
    }

}
