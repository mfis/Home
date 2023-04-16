package de.fimatas.home.library.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecast implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime time;

    private BigDecimal temperature;

    private BigDecimal wind;

    private BigDecimal gust;

    private boolean isDay;

    private BigDecimal precipitationInMM;

    private BigDecimal sunshineInMin;

    private Set<WeatherConditions> icons;
}
