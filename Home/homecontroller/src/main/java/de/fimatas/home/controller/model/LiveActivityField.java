package de.fimatas.home.controller.model;

import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.model.ConditionColor;
import de.fimatas.home.library.util.ViewFormatterUtils;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.function.Function;

import static de.fimatas.home.library.util.HomeUtils.buildDecimalFormat;

@Getter
public enum LiveActivityField {

    HOUSE_CONSUMPTION(
            val -> "house.fill",
            "sys",
            new BigDecimal(50),
            true,
            val -> buildDecimalFormat("0.0").format(val.abs().divide(new BigDecimal(1000), new MathContext(3, RoundingMode.HALF_UP)))+ "kW",
            val -> buildDecimalFormat("0.0").format(val.abs().divide(new BigDecimal(1000), new MathContext(3, RoundingMode.HALF_UP))),
            val -> ViewFormatterUtils.mapAppColorAccent(ConditionColor.DEFAULT.getUiClass())
    ), //

    PV_PRODUCTION(
            val -> "sun.max.fill",
            "sys",
            new BigDecimal(50),
            true,
            val -> buildDecimalFormat("0.0").format(val.abs().divide(new BigDecimal(1000), new MathContext(3, RoundingMode.HALF_UP))) + "kW",
            val -> buildDecimalFormat("0.0").format(val.abs().divide(new BigDecimal(1000), new MathContext(3, RoundingMode.HALF_UP))),
            val -> ViewFormatterUtils.mapAppColorAccent(val.compareTo(BigDecimal.ZERO) > 0 ? ConditionColor.GREEN.getUiClass() : ConditionColor.DEFAULT.getUiClass())
    ), //

    EV_CHARGE(
            val -> "bolt.car",
            "sys",
            new BigDecimal(1),
            false,
            val -> val.intValue() + "%",
            val -> val.intValue() + "%",
            val -> ViewFormatterUtils.mapAppColorAccent(ViewFormatterUtils.calculateViewConditionColorEv(val.shortValue()).getUiClass())
    ), //

    PV_BATTERY(
            val -> {
                final int soc = ModelObjectDAO.getInstance().readPvAdditionalDataModel().getBatteryStateOfCharge();
                if(soc < 15){
                    return "battery.0percent";
                } else if(soc < 40){
                    return "battery.25percent";
                } else if(soc < 65){
                    return "battery.50percent";
                } else if(soc < 90){
                    return "battery.75percent";
                } else {
                    return "battery.100percent";
                }
            },
            "sys",
            new BigDecimal(1),
            false,
            val -> val.intValue() + "%",
            val -> val.intValue() + "%",
            val -> {
                switch (ModelObjectDAO.getInstance().readPvAdditionalDataModel().getPvBatteryState()){
                    case CHARGING -> {
                        return ViewFormatterUtils.mapAppColorAccent(ConditionColor.BLUE.getUiClass());
                    }
                    case DISCHARGING -> {
                        return ViewFormatterUtils.mapAppColorAccent(ConditionColor.GREEN.getUiClass());
                    }
                    default -> {
                        return ViewFormatterUtils.mapAppColorAccent(ConditionColor.DEFAULT.getUiClass());
                    }
                }
            }
    ), //

    ;

    private final Function<BigDecimal, String> symbolName;

    private final String symbolType;

    private final BigDecimal thresholdMin;

    private final boolean allowsHighPriority;

    private final Function<BigDecimal, String> formatterValue;

    private final Function<BigDecimal, String> formatterShort;

    private final Function<BigDecimal, String> color;

    LiveActivityField(Function<BigDecimal, String> symbolName, String symbolType, BigDecimal thresholdMin, boolean allowsHighPriority, Function<BigDecimal, String> formatterValue, Function<BigDecimal, String> formatterShort, Function<BigDecimal, String> color){
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
