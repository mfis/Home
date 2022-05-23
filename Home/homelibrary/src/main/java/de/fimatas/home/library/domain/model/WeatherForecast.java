package de.fimatas.home.library.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecast implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime time;

    private BigDecimal temperature;

    private BigDecimal wind;

    private boolean isDay;

    private List<WeatherConditions> icons;
}
