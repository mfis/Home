package de.fimatas.home.controller.database.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import de.fimatas.home.controller.model.HistoryValueType;

public class TimestampValuePair {

    private LocalDateTime timestamp;

    private BigDecimal value;

    private HistoryValueType type;

    public TimestampValuePair(LocalDateTime timestamp, BigDecimal value, HistoryValueType type) {
        super();
        this.timestamp = timestamp;
        this.value = value;
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BigDecimal getValue() {
        return value;
    }

    public HistoryValueType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "TimestampValuePair [timestamp=" + timestamp + ", value=" + value + ", type=" + type + "]";
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

}
