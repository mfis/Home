package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class WeatherForecastModel extends AbstractSystemModel {

    private String sourceText;

    private List<WeatherForecast> forecasts = new LinkedList<>();

    private Map<LocalDate, WeatherForecastConclusion> conclusionForDate = new LinkedHashMap<>();

    private WeatherForecastConclusion conclusionToday;

    private WeatherForecastConclusion conclusionTomorrow;

    private WeatherForecastConclusion conclusion24to48hours;

    private WeatherForecastConclusion conclusion3hours;

    private Map<LocalDate, WeatherForecastConclusion> furtherDaysConclusion = new LinkedHashMap<>();

    private Map<LocalDate, List<WeatherForecast>> furtherDaysForecasts = new LinkedHashMap<>();
}
