package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class WeatherForecastConclusion implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal minTemp;
    private BigDecimal maxTemp;
    private Integer maxWind;
    private List<WeatherConditions> conditions = new LinkedList<>();
    private Map<WeatherConditions, LocalDateTime> firstOccurences = new HashMap<>();

}
