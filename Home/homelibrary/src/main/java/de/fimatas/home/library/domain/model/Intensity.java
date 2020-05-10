package de.fimatas.home.library.domain.model;

public enum Intensity {

    NO("", ""), //
    LOW("Leicht sonnig", "Leicht aufgeheizt"), //
    MEDIUM("Sonnig", "Aufgeheizt"), //
    HIGH("Stark sonnig", "Stark aufgeheizt"), //
    ;

    private String sun;

    private String heating;

    private Intensity(String sun, String heating) {
        this.sun = sun;
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

    public String getSun() {
        return sun;
    }

    public String getHeating() {
        return heating;
    }
}
