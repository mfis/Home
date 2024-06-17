package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class WeatherForecastConclusion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal minTemp;
    private BigDecimal maxTemp;
    private Integer maxWind;
    private Integer maxGust;
    private BigDecimal precipitationInMM;
    private Integer precipitationProbability;
    private BigDecimal sunshineInMin;
    private Set<WeatherConditions> conditions;
    private Map<WeatherConditions, LocalDateTime> firstOccurences = new HashMap<>();
    private boolean forecast;

    public static WeatherForecastConclusion fromWeatherForecast(WeatherForecast wf){
        var conclusion = new WeatherForecastConclusion();
        conclusion.setForecast(true);
        conclusion.setMinTemp(wf.getTemperature());
        conclusion.setMaxTemp(wf.getTemperature());
        conclusion.setMaxWind(wf.getWind().setScale(0, RoundingMode.HALF_UP).intValue());
        conclusion.setMaxGust(wf.getGust().setScale(0, RoundingMode.HALF_UP).intValue());
        conclusion.setPrecipitationInMM(wf.getPrecipitationInMM());
        conclusion.setPrecipitationProbability(wf.getPrecipitationProbability());
        conclusion.setSunshineInMin(wf.getSunshineInMin());
        conclusion.setConditions(wf.getIcons());
        return conclusion;
    }

    public static WeatherForecastConclusion fromSingleTemperature(BigDecimal temperature){
        var conclusion = new WeatherForecastConclusion();
        conclusion.setForecast(false);
        conclusion.setMinTemp(temperature);
        conclusion.setMaxTemp(temperature);
        conclusion.setConditions(Set.of());
        return conclusion;
    }

}
