package de.fimatas.home.library.util;

import java.math.BigDecimal;

public class HomeAppConstants {

    private HomeAppConstants() {
        // noop
    }

    //

    public static final String CONTROLLER_CLIENT_COMM_TOKEN = "homeAppControllerClientCommunicationToken";

    public static final String PUSH_TOKEN_NOT_AVAILABLE_INDICATOR = "n/a";

    //

    public static final int CONTROLLER_CLIENT_LONGPOLLING_REQUEST_TIMEOUT_SECONDS = 20;

    public static final int CONTROLLER_CLIENT_LONGPOLLING_RESPONSE_TIMEOUT_SECONDS = 10;

    //

    public static final BigDecimal TARGET_HUMIDITY_MIN_INSIDE = new BigDecimal("40");

    public static final BigDecimal TARGET_HUMIDITY_MAX_INSIDE = new BigDecimal("70");

    //

    public static final int MODEL_OUTDATED_SECONDS = 60 * 10;

    public static final int MODEL_DEFAULT_INTERVAL_SECONDS = 10;

    public static final int MODEL_MAX_UPDATE_INTERVAL_SECONDS = 60 * 4;

    public static final int MODEL_UPDATE_WARNING_SECONDS = 60 * 6;

    //

    public static final int DEVICE_STATE_INTERVAL_SECONDS = 60 * 3;

    //

    public static final int POWER_CONSUMPTION_OUTDATED_DECONDS = 60 * 7;

    //

    public static final int HISTORY_OUTDATED_SECONDS = 60 * 15;

    public static final int HISTORY_DEFAULT_INTERVAL_SECONDS = 60 * 5;
}
