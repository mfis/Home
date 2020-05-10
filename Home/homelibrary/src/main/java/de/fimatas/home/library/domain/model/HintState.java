package de.fimatas.home.library.domain.model;

public enum HintState {

    OFF(""), //
    OFF_HYSTERESIS("ggf."), //
    ACTIVE_HYSTERESIS("Empfehlung:"), //
    ACTIVE("Empfehlung:"), //
    ;

    // up: O -> OH -> A
    // dn: A -> AH -> O
    // on all: OH,A
    // on hys: AH,A

    private final String textualPrefix;

    private HintState(String textualPrefix) {
        this.textualPrefix = textualPrefix;
    }

    private static final long HYST_TIME_UP = 1000L * 60 * 4;

    private static final long HYST_TIME_DOWN = 1000L * 60 * 20;

    public static HintState newState() {
        return OFF_HYSTERESIS;
    }

    public HintState up(long diffMillies) {
        switch (this) {
        case OFF:
            return OFF_HYSTERESIS;
        case OFF_HYSTERESIS:
            return diffMillies >= HYST_TIME_UP ? ACTIVE : OFF_HYSTERESIS;
        case ACTIVE_HYSTERESIS:
        case ACTIVE:
            return ACTIVE;
        default:
            return OFF;
        }
    }

    public HintState down(long diffMillies) {
        switch (this) {
        case ACTIVE:
            return ACTIVE_HYSTERESIS;
        case ACTIVE_HYSTERESIS:
            return diffMillies >= HYST_TIME_DOWN ? OFF : HintState.ACTIVE_HYSTERESIS;
        case OFF_HYSTERESIS:
            return diffMillies >= HYST_TIME_DOWN ? OFF : HintState.OFF_HYSTERESIS;
        case OFF:
            return OFF;
        default:
            return OFF;
        }
    }

    public String getTextualPrefix() {
        return textualPrefix;
    }

}
