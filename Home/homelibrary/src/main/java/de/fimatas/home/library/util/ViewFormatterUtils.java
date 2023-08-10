package de.fimatas.home.library.util;

import de.fimatas.home.library.domain.model.ElectricVehicleState;

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
}
