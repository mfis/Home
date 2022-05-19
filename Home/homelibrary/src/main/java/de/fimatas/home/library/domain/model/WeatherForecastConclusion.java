package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Data
public class WeatherForecastConclusion implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal minTemp;
    private BigDecimal maxTemp;
    private Integer maxWind;
    private List<WeatherConditions> conditions;

    public static int formatTemperature(BigDecimal temperature){
        return temperature.setScale(0, RoundingMode.HALF_UP).intValue();
    }

}
