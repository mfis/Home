package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class WeatherForecastView {

    private String time;

    private String temperature;

    private String wind;

    private List<String> icons = new LinkedList<>();
}
