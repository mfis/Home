package de.fimatas.home.library.domain.model;

public enum Intensity {

    NO(""), //
    LOW( "Leicht aufgeheizt"), //
    MEDIUM( "Aufgeheizt"), //
    HIGH( "Stark aufgeheizt"), //
    ;

    private final String heating;

    Intensity(String heating) {
        this.heating = heating;
    }

    public static Intensity max(Intensity a, Intensity b) {
        int max = Math.max(a.ordinal(), b.ordinal());
        for (Intensity i : values()) {
            if (max == i.ordinal()) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    public String getHeating() {
        return heating;
    }
}
