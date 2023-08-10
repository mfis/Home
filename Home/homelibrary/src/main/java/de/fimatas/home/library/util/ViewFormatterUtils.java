package de.fimatas.home.library.util;

import de.fimatas.home.library.domain.model.ElectricVehicleState;
import de.fimatas.home.library.model.ConditionColor;

public class ViewFormatterUtils {

    public static String calculateViewFormattedPercentageEv(ElectricVehicleState evs) {
        var isChargedSinceReading = evs.getChargingTimestamp() != null;
        var percentagePrefix = isChargedSinceReading?"~":"";
        var percentage = calculateViewPercentageEv(evs);
        return percentagePrefix + percentage + "%";
    }

    public static short calculateViewPercentageEv(ElectricVehicleState evs) {
        if (evs.getChargingTimestamp() != null) {
            var s = (short) (evs.getBatteryPercentage() + evs.getAdditionalChargingPercentage());
            return s>100?100:s;
        } else {
            return evs.getBatteryPercentage();
        }
    }

    public static ConditionColor calculateViewConditionColorEv(short percentage) {
        return percentage > 89 ? ConditionColor.ORANGE:percentage<21?ConditionColor.RED:ConditionColor.GREEN; // TODO: constant
    }

    public static String mapAppColorAccent(String colorClass) {

        final ConditionColor conditionColor = ConditionColor.fromUiName(colorClass);
        if(conditionColor==null){
            return "";
        }

        switch (conditionColor) {
            case GREEN:
                return ".green";
            case ORANGE:
                return ".orange";
            case RED:
                return ".red";
            case BLUE:
                return ".blue";
            case LIGHT:
            case COLD:
                return ".purple";
            default:
                return "";
        }
    }
}
