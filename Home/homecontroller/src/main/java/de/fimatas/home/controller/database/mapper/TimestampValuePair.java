package de.fimatas.home.controller.database.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import de.fimatas.home.controller.model.HistoryValueType;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TimestampValuePair {

    @Setter
    private LocalDateTime timestamp;

    private final BigDecimal value;

    private final HistoryValueType type;

    public TimestampValuePair(LocalDateTime timestamp, BigDecimal value, HistoryValueType type) {
        super();
        this.timestamp = timestamp;
        this.value = value;
        this.type = type;
    }

    @Override
    public String toString() {
        return "TimestampValuePair [timestamp=" + timestamp + ", value=" + value + ", type=" + type + "]";
    }

}
