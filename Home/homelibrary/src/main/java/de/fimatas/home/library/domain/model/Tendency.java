package de.fimatas.home.library.domain.model;

import java.math.BigDecimal;

public enum Tendency {

    NONE(0, "", ""), //

    RISE(0, "far fa-arrow-alt-circle-up", "arrow.up.circle"), //
    RISE_SLIGHT(Constants.ONE_MINUTE * 25, "far fa-arrow-alt-circle-up fa-rotate-45", "arrow.up.forward.circle"), //
    EQUAL(Constants.ONE_MINUTE * 75, "far fa-arrow-alt-circle-right", "equal.circle"), //
    FALL_SLIGHT(Constants.ONE_MINUTE * 25, "far fa-arrow-alt-circle-right fa-rotate-45", "arrow.down.right.circle"), //
    FALL(0, "far fa-arrow-alt-circle-down", "arrow.down.circle"), //
    ;

    private long timeDiff;

    private String iconCssClass;

    private String symbolId;

    private Tendency(long timeDiff, String iconCssClass, String symbolId) {
        this.timeDiff = timeDiff;
        this.iconCssClass = iconCssClass;
        this.symbolId = symbolId;
    }

    public static Tendency calculate(ValueWithTendency<BigDecimal> reference, long timeDiff) {

        if (reference.getTendency() == null) {
            return Tendency.EQUAL;
        }

        switch (reference.getTendency()) {
        case RISE:
            if (timeDiff >= Tendency.EQUAL.getTimeDiff()) {
                return Tendency.EQUAL;
            } else if (timeDiff >= Tendency.RISE_SLIGHT.getTimeDiff()) {
                return Tendency.RISE_SLIGHT;
            }
            break;
        case FALL:
            if (timeDiff >= Tendency.EQUAL.getTimeDiff()) {
                return Tendency.EQUAL;
            } else if (timeDiff >= Tendency.FALL_SLIGHT.getTimeDiff()) {
                return Tendency.FALL_SLIGHT;
            }
            break;
        case RISE_SLIGHT:
            if (timeDiff >= Tendency.EQUAL.getTimeDiff()) {
                return Tendency.EQUAL;
            }
            break;
        case FALL_SLIGHT:
            if (timeDiff >= Tendency.EQUAL.getTimeDiff()) {
                return Tendency.EQUAL;
            }
            break;
        default:
            return Tendency.EQUAL;
        }

        return reference.getTendency();
    }

    public static class Constants {

        private Constants() {
            super();
        }

        public static final long ONE_MINUTE = 1000L * 60L;
    }

    public static String nameFromCssClass(String cssClass) {
        for (Tendency tendency : values()) {
            if (tendency.getIconCssClass().equals(cssClass)) {
                return tendency.name();
            }
        }
        return NONE.name();
    }

    public static String symbolFromCssClass(String cssClass) {
        for (Tendency tendency : values()) {
            if (tendency.getIconCssClass().equals(cssClass)) {
                return tendency.symbolId;
            }
        }
        return NONE.name();
    }

    public long getTimeDiff() {
        return timeDiff;
    }

    public String getIconCssClass() {
        return iconCssClass;
    }
}
