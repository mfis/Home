package de.fimatas.home.library.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class EvChargingTime {

    private short amperage;

    private short phaseCount;

    private short voltage;

    private int minutes;
}
