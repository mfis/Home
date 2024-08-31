package de.fimatas.home.library.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

public class PowerConsumptionDay implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient LocalDateTime measurePointMaxDateTime = null;

    @Getter
    private long measurePointMax;

    @Setter
    @Getter
    private Map<TimeRange, BigDecimal> values;

    public PowerConsumptionDay() {
        values = new LinkedHashMap<>();
    }

    public BigDecimal getSum(){
        return values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public LocalDateTime measurePointMaxDateTime() {
        if (measurePointMaxDateTime == null) {
            measurePointMaxDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(measurePointMax), ZoneId.systemDefault());
        }
        return measurePointMaxDateTime;
    }

    public void setMeasurePointMax(long measurePointMax) {
        this.measurePointMax = measurePointMax;
        measurePointMaxDateTime = null;
    }
}
