package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class WeatherForecastView extends View {

    private String header = "";

    private String dayNight = "";

    private String time;

    private String detailKey = "";

    private String detailCaption = "";

    private String temperature;

    private List<ValueWithCaption> icons = new LinkedList<>();

    private List<WeatherForecastView> detailForecasts = new LinkedList<>();
}
