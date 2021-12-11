package de.fimatas.home.library.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class TemperatureHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private long date;

    private boolean singleDay;

    private BigDecimal min;

    private BigDecimal max;

    public boolean empty() {
        return min == null && max == null;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isSingleDay() {
        return singleDay;
    }

    public void setSingleDay(boolean singleDay) {
        this.singleDay = singleDay;
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal nightMin) {
        this.min = nightMin;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal nightMax) {
        this.max = nightMax;
    }
}
