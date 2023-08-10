package de.fimatas.home.controller.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.function.Function;

@Getter
public enum LiveActivityField {

    // TODO: Re-use View Formatter
    ELECTRIC_GRID(new BigDecimal(50),
            val -> new DecimalFormat("0").format(val) + "W",
            val -> new DecimalFormat("0.0").format(val.divide(new BigDecimal(1000), new MathContext(3, RoundingMode.HALF_UP)))), //

    EV_CHARGE(new BigDecimal(1),
            val -> val.intValue() + "%",
            val -> val.intValue() + "%"), //
    ;

    private final BigDecimal thresholdMin;

    private final Function<BigDecimal, String> formatterValue;

    private final Function<BigDecimal, String> formatterShort;

    LiveActivityField(BigDecimal thresholdMin, Function<BigDecimal, String> formatterValue, Function<BigDecimal, String> formatterShort){
        this.thresholdMin = thresholdMin;
        this.formatterValue = formatterValue;
        this.formatterShort = formatterShort;
    }

    public String formatValue(BigDecimal value){
        return formatterValue.apply(value);
    }

    public String formatShort(BigDecimal value){
        return formatterShort.apply(value);
    }
}
