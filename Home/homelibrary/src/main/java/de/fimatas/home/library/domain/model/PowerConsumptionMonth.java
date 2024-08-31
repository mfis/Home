package de.fimatas.home.library.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class PowerConsumptionMonth implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    private BigDecimal powerConsumption;

    @Getter
    private long measurePointMax;

    private transient LocalDateTime measurePointMaxDateTime = null;

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
