package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Data
public class WeatherForecastModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private long dateTime;

    private String sourceText;

    private List<WeatherForecast> forecasts = new LinkedList<>();

    private WeatherForecastConclusion conclusionToday;

    private WeatherForecastConclusion conclusion24to48hours;
}
