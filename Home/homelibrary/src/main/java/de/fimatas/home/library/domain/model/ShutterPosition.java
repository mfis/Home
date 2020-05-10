package de.fimatas.home.library.domain.model;

public enum ShutterPosition {

    OPEN(0, Integer.MIN_VALUE, 5, "Geöffnet", false, "far fa-square"), //
    HALF(40, 6, 85, "% unten", true, "far fa-window-maximize"), //
    SUNSHADE(95, 86, 97, "Sonnenschutz", false, "fas fa-th"), //
    CLOSE(100, 98, Integer.MAX_VALUE, "Geschlossen", false, "fas fa-square"), //
    ;

    private int controlPosition;

    private int positionRangeStart;

    private int positionRangeEnd;

    private String text;

    private boolean formatWithPercentage;

    private String icon;

    private ShutterPosition(int controlPosition, int positionRangeStart, int positionRangeEnd, String text,
            boolean formatWithPercentage, String icon) {
        this.controlPosition = controlPosition;
        this.positionRangeStart = positionRangeStart;
        this.positionRangeEnd = positionRangeEnd;
        this.text = text;
        this.formatWithPercentage = formatWithPercentage;
        this.icon = icon;
    }

    public static ShutterPosition fromPosition(int position) {
        for (ShutterPosition shutterPosition : ShutterPosition.values()) {
            if (position >= shutterPosition.positionRangeStart && position <= shutterPosition.positionRangeEnd) {
                return shutterPosition;
            }
        }
        return null;
    }

    public String getText(int percentage) {
        if (formatWithPercentage) {
            return percentage + text;
        } else {
            return text;
        }
    }

    public int getControlPosition() {
        return controlPosition;
    }

    public String getIcon() {
        return icon;
    }

}
