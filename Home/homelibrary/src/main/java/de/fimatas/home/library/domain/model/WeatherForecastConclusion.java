package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class WeatherForecastConclusion implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal minTemp;
    private BigDecimal maxTemp;
    private Integer maxWind;
    private Integer maxGust;
    private BigDecimal precipitationInMM;
    private BigDecimal sunshineInMin;
    private Set<WeatherConditions> conditions;
    private Map<WeatherConditions, LocalDateTime> firstOccurences = new HashMap<>();

    public static WeatherForecastConclusion fromWeatherForecast(WeatherForecast wf){
        var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(wf.getTemperature());
        conclusion.setMaxTemp(wf.getTemperature());
        conclusion.setMaxWind(wf.getWind().setScale(0, RoundingMode.HALF_UP).intValue());
        conclusion.setMaxGust(wf.getGust().setScale(0, RoundingMode.HALF_UP).intValue());
        conclusion.setPrecipitationInMM(wf.getPrecipitationInMM());
        conclusion.setSunshineInMin(wf.getSunshineInMin());
        conclusion.setConditions(wf.getIcons());
        return conclusion;
    }

}
