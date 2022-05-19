package de.fimatas.home.client.domain.model;

import de.fimatas.home.library.model.ConditionColor;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class WeatherForecastView extends View {

    private String header = "";

    private String stripeColorClass = ConditionColor.ROW_STRIPE_DEFAULT.getUiClass();

    private String time;

    private String temperature;

    private String wind;

    private List<String> icons = new LinkedList<>();
}
