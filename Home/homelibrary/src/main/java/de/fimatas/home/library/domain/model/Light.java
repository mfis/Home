package de.fimatas.home.library.domain.model;

import java.io.Serializable;

public class Light implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String id;

    private Place place;

    private LightState state;

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        this.place = place;
    }

    public LightState getState() {
        return state;
    }

    public void setState(LightState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
