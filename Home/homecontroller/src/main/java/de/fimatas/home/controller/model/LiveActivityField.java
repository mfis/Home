package de.fimatas.home.controller.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.function.Function;

@Getter
public enum LiveActivityField {

    ELECTRIC_GRID(new BigDecimal(50), val -> "format_1_" + val.toString()), //
    EV_CHARGE(new BigDecimal(1), val -> "format_2_" + val.toString()), //
    ;

    private final BigDecimal thresholdMin;

    private final Function<BigDecimal, String> formatter;

    LiveActivityField(BigDecimal thresholdMin, Function<BigDecimal, String> formatter){
        this.thresholdMin = thresholdMin;
        this.formatter = formatter;
    }

    public String format(BigDecimal value){
        return formatter.apply(value);
    }

}
