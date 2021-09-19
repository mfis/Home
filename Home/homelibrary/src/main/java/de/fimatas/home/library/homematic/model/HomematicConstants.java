package de.fimatas.home.library.homematic.model;

import java.math.BigDecimal;

public final class HomematicConstants {

    private HomematicConstants() {
        super();
    }

    public static final BigDecimal HEATING_CONTROL_MODE_BOOST = new BigDecimal(3);

    public static final BigDecimal HEATING_CONTROL_MODE_MANUAL = new BigDecimal(1);

    public static final BigDecimal HEATING_CONTROL_MODE_AUTO = new BigDecimal(0);
}
