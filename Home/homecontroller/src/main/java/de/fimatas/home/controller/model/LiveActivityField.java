package de.fimatas.home.controller.model;

import de.fimatas.home.library.model.ConditionColor;
import de.fimatas.home.library.util.ViewFormatterUtils;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.function.Function;

import static de.fimatas.home.library.util.HomeUtils.buildDecimalFormat;

@Getter
public enum LiveActivityField {

    // TODO: Re-use View Formatter
    ELECTRIC_GRID(
            "energygrid", "app",
            new BigDecimal(40),
            true,
            val -> buildDecimalFormat("0").format(val.abs()) + " W",
            val -> buildDecimalFormat("0.0").format(val.abs().divide(new BigDecimal(1000), new MathContext(3, RoundingMode.HALF_UP))),
            val -> ViewFormatterUtils.mapAppColorAccent(val.compareTo(BigDecimal.ZERO) > 0 ? ConditionColor.ORANGE.getUiClass() : ConditionColor.GREEN.getUiClass())
    ), //

    EV_CHARGE(
            "bolt.car", "sys",
            new BigDecimal(1),
            false,
            val -> val.intValue() + "%",
            val -> val.intValue() + "%",
            val -> ViewFormatterUtils.mapAppColorAccent(ViewFormatterUtils.calculateViewConditionColorEv(val.shortValue()).getUiClass())
    ), //
    ;

    private final String symbolName;

    private final String symbolType;

    private final BigDecimal thresholdMin;

    private final boolean allowsHighPriority;

    private final Function<BigDecimal, String> formatterValue;

    private final Function<BigDecimal, String> formatterShort;

    private final Function<BigDecimal, String> color;

    LiveActivityField(String symbolName, String symbolType, BigDecimal thresholdMin, boolean allowsHighPriority, Function<BigDecimal, String> formatterValue, Function<BigDecimal, String> formatterShort, Function<BigDecimal, String> color){
        this.symbolName = symbolName;
        this.symbolType = symbolType;
        this.thresholdMin = thresholdMin;
        this.allowsHighPriority = allowsHighPriority;
        this.formatterValue = formatterValue;
        this.formatterShort = formatterShort;
        this.color = color;
    }

    public String formatValue(BigDecimal value){
        return formatterValue.apply(value);
    }

    public String formatShort(BigDecimal value){
        return formatterShort.apply(value);
    }

    public String color(BigDecimal value){
        return color.apply(value);
    }
}
