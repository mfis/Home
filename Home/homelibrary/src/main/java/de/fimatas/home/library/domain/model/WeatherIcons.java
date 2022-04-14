package de.fimatas.home.library.domain.model;

public enum WeatherIcons{

    SUN("fa-solid fa-sun"),
    MOON("fa-solid fa-moon"),
    SUN_CLOUD("fa-solid fa-cloud-sun"),
    MOON_CLOUD("fa-solid fa-cloud-moon"),
    CLOUD_RAIN("fa-solid fa-cloud-rain"),
    CLOUD("fa-solid fa-cloud"),
    RAIN("fa-solid fa-cloud-showers-heavy"),
    FOG("fa-solid fa-smog"),
    WIND("fa-solid fa-wind"),
    SNOW("fa-solid fa-snowflake"),
    HAIL("fa-solid fa-cloud-meatball"),
    THUNDERSTORM("fa-solid fa-cloud-bolt"),
    UNKNOWN("fa-solid fa-circle-question")
    ;

    private final String fontAwesomeID;
    WeatherIcons(String fontAwesomeID){
        this.fontAwesomeID = fontAwesomeID;
    }
    public String getFontAwesomeID() {
        return fontAwesomeID;
    }
}
