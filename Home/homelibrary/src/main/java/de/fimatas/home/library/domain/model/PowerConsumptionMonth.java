package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class PowerConsumptionMonth implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long powerConsumption;

    private long measurePointMin;

    private long measurePointMax;

    private transient LocalDateTime measurePointMaxDateTime = null;

    public Long getPowerConsumption() {
        return powerConsumption;
    }

    public void setPowerConsumption(Long powerConsumption) {
        this.powerConsumption = powerConsumption;
    }

    public long getMeasurePointMin() {
        return measurePointMin;
    }

    public void setMeasurePointMin(long measurePointMin) {
        this.measurePointMin = measurePointMin;
    }

    public long getMeasurePointMax() {
        return measurePointMax;
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
