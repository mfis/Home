package de.fimatas.home.controller.model;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum LiveActivityField {

    ELECTRIC_GRID(new BigDecimal(50)), //
    ;

    private final BigDecimal thresholdMin;

    LiveActivityField(BigDecimal thresholdMin){
        this.thresholdMin = thresholdMin;
    }
}
