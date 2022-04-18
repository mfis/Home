package de.fimatas.home.library.domain.model;

public enum WeatherIcons{

    SUN("fa-solid fa-sun", "sun.max.fill", "Sonne"),
    MOON("fa-solid fa-moon", "moon.fill", "Mond"),
    SUN_CLOUD("fa-solid fa-cloud-sun", "cloud.sun.fill", "Leicht bewölkt"),
    MOON_CLOUD("fa-solid fa-cloud-moon", "cloud.moon.fill", "Leicht bewölkt"),
    CLOUD_RAIN("fa-solid fa-cloud-rain", "cloud.rain.fill", "Bewölkt und Regen"),
    CLOUD("fa-solid fa-cloud", "cloud.fill", "Wolken"),
    RAIN("fa-solid fa-cloud-showers-heavy", "cloud.heavyrain.fill", "Regen"),
    FOG("fa-solid fa-smog", "cloud.fog.fill", "Nebel"),
    WIND("fa-solid fa-wind",  "wind","Sturm"),
    SNOW("fa-solid fa-snowflake", "snowflake.circle.fill", "Schnee"),
    HAIL("fa-solid fa-cloud-meatball", "cloud.hail.fill", "Hagel"),
    THUNDERSTORM("fa-solid fa-cloud-bolt", "bolt.fill", "Gewitter"),
    UNKNOWN("fa-solid fa-circle-question", "", "Unbekannt")
    ;

    private final String fontAwesomeID;
    private final String sfSymbolsID;
    private final String caption;
    WeatherIcons(String fontAwesomeID, String sfSymbolsID, String caption){
        this.fontAwesomeID = fontAwesomeID;
        this.sfSymbolsID = sfSymbolsID;
        this.caption = caption;
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
}
