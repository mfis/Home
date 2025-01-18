package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class WeatherForecastsView extends View {

    private List<WeatherForecastView> forecasts = new LinkedList<>();

    private String source;

    private String stateTemperatureWatch = "";

    private String stateEventWatch = "";

    private String shortTermText = "";

    private String shortTermColorClass = "";
}
