package de.fimatas.home.library.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecast implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public WeatherForecast(WeatherForecast other){
        this.time = LocalDateTime.from(other.time);
        this.temperature = other.temperature;
        this.wind = other.wind;
        this.gust = other.gust;
        this.isDay = other.isDay;
        this.precipitationInMM = other.precipitationInMM;
        this.precipitationProbability = other.precipitationProbability;
        this.sunshineInMin = other.sunshineInMin;
        this.icons = new HashSet<>(other.icons);
    }

    private LocalDateTime time;

    private BigDecimal temperature;

    private BigDecimal wind;

    private BigDecimal gust;

    private boolean isDay;

    private BigDecimal precipitationInMM;

    private Integer precipitationProbability;

    private BigDecimal sunshineInMin;

    private Set<WeatherConditions> icons;
}
