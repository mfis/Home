package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class WeatherForecastsView extends View{

    private List<WeatherForecastView> forecasts = new LinkedList<>();

    private String source;
}
