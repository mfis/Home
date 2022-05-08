package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WeatherForecastConclusion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer minTemp;
    private Integer maxTemp;
    private Integer maxWind;
    private List<WeatherConditions> conditions;

}
