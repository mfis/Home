package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WeatherForecast implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime time;

    private BigDecimal temperature;

    private BigDecimal wind;

    private List<WeatherIcons> icons;
}
