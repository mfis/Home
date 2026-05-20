package de.fimatas.home.library.model;

import de.fimatas.home.library.domain.model.Place;
import lombok.Getter;

public enum PersistentCacheKey {

    HEATPUMP_ROOF(Place.ROOF, "Status Wärmepumpe Dach"),
    ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE(Place.HOUSE, "Stromverbrauch Haus gesamt"),
    ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE(Place.HOUSE, "Stromproduktion Haus gesamt"),
    ELECTRIC_POWER_PRODUCTION_ACTUAL_HOUSE(Place.HOUSE, "Stromproduktion Haus aktuell"),
    ELECTRIC_POWER_CONSUMPTION_ACTUAL_HOUSE(Place.HOUSE, "Stromverbrauch Haus aktuell"),
    ELECTRIC_POWER_ACTUAL_TIMESTAMP_HOUSE(Place.HOUSE, "Stromverbrauch Zeitpunkt"),
    ELECTRIC_POWER_CONSUMPTION_COUNTER_HEATPUMP_BASEMENT(Place.BASEMENT, "Wärmepumpe Keller Verbrauch gesamt"),
    WARMTH_POWER_PRODUCTION_COUNTER_HEATPUMP_BASEMENT(Place.BASEMENT, "Wärmepumpe Keller Wärmeproduktion gesamt"),
    ;

    @Getter
    private final Place place;

    @Getter
    private final String description;

    PersistentCacheKey(Place place, String description) {
        this.place = place;
        this.description = description;
    }
}
