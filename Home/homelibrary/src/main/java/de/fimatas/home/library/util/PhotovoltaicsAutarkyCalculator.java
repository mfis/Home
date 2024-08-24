package de.fimatas.home.library.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PhotovoltaicsAutarkyCalculator {

    public static int calculateAutarkyPercentage(BigDecimal consumption, BigDecimal gridPurchased) {

        // null-safe: comsumption
        if (consumption == null) {
            consumption = BigDecimal.ZERO;
        }

        // null-safe: gridPurchased
        if (gridPurchased == null) {
            gridPurchased = BigDecimal.ZERO;
        }

        // if consumption is null, set it to the grid purchased value
        if (consumption.compareTo(BigDecimal.ZERO) == 0) {
            consumption = gridPurchased;
        }

        // if consumption is zero or null, autarky is 100% to avoid division by zero
        if (consumption.compareTo(BigDecimal.ZERO) == 0) {
            return 100;
        }

        // calculate self-consumed energy
        BigDecimal selfConsumption = consumption.subtract(gridPurchased);

        // calculate autarky percentage: (Self-consumption / Consumption) * 100
        BigDecimal autarky = selfConsumption.multiply(BigDecimal.valueOf(100)).divide(consumption, 2, RoundingMode.HALF_UP);

        // ensure that autarky is never negative
        if (autarky.compareTo(BigDecimal.ZERO) < 0) {
            autarky = BigDecimal.ZERO;
        }

        return autarky.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
