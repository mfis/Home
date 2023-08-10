package de.fimatas.home.controller.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class LiveActivityModel {

    private String username;

    private String token;

    private String device;

    private LiveActivityType liveActivityType;

    private LocalDateTime lastValTimestampHighPriority;

    private LocalDateTime lastValTimestampLowPriority;

    private int updateCounter;

    private Map<LiveActivityField, BigDecimal> lastValuesSentWithHighPriotity = new HashMap<>();

    private Map<LiveActivityField, BigDecimal> actualValues = new HashMap<>();

    public void shiftValuesToSentWithHighPriotity(){
        lastValuesSentWithHighPriotity.clear();
        lastValuesSentWithHighPriotity.putAll(actualValues);
    }
}
