package de.fimatas.home.library.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum Place {

    LIVINGROOM("Wohnzimmer", false), //
    KITCHEN("Küche", false), //
    KIDSROOM_1("Kinderzimmer", true), //
    KIDSROOM_2("Kinderzimmer", true), //
    BATHROOM("Bad", true), //
    BEDROOM("Schlafzimmer", true), //
    LAUNDRY("Waschküche", false), //
    GUESTROOM("Gästezimmer", false), //
    WORKSHOP("Werkstatt", false), //
    HOUSE("Haus", false), //
    WALLBOX("Wallbox", false), //
    ENTRANCE("Einfahrt", false), //
    TERRACE("Terrasse", false), //
    GARDEN("Garten", false), //
    FRONTDOOR("Haustür", false), //
    // with sub-places
    OUTSIDE("Draußen", false, Place.ENTRANCE, Place.TERRACE), //
    ;

    private String placeName;

    private boolean airCondition;

    private final List<Place> subPlaces = new ArrayList<>();

    private Place(String placeName, boolean airCondition, Place... subPlaces) {
        this.placeName = placeName;
        this.airCondition = airCondition;
        if (subPlaces != null) {
            this.subPlaces.addAll(Arrays.asList(subPlaces));
        }
    }

    public String getPlaceName() {
        return placeName;
    }

    public List<Place> getSubPlaces() {
        return subPlaces;
    }

    public boolean isAirCondition() {
        return airCondition;
    }

    public List<Place> allPlaces() {
        List<Place> list = new LinkedList<>();
        list.add(this);
        list.addAll(subPlaces);
        return list;
    }

    public static Place fromName(String groupName) {
        for (Place place : values()) {
            if (place.getPlaceName().equalsIgnoreCase(groupName)) {
                return place;
            }
        }
        return null;
    }

}
