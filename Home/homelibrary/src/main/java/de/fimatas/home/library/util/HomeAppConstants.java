package de.fimatas.home.library.util;

import java.math.BigDecimal;

public class HomeAppConstants {

    private HomeAppConstants() {
        // noop
    }

    public static final String CONTROLLER_CLIENT_COMM_TOKEN = "homeAppControllerClientCommunicationToken";

    public static final int CONTROLLER_CLIENT_LONGPOLLING_REQUEST_TIMEOUT_SECONDS = 20;

    public static final int CONTROLLER_CLIENT_LONGPOLLING_RESPONSE_TIMEOUT_SECONDS = 10;

    public static final BigDecimal TARGET_HUMIDITY_MIN_INSIDE = new BigDecimal("40");

    public static final BigDecimal TARGET_HUMIDITY_MAX_INSIDE = new BigDecimal("70");

}
