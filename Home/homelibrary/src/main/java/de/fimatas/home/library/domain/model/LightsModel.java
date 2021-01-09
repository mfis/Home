package de.fimatas.home.library.domain.model;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LightsModel {

    private Map<Place, List<Light>> lightsMap = new EnumMap<>(Place.class);

    private long timestamp;

    public LightsModel() {
        setTimestamp(System.currentTimeMillis());
    }

    public void addLight(Light light) {
        if (!lightsMap.containsKey(light.getPlace())) {
            lightsMap.put(light.getPlace(), new LinkedList<>());
        }
        lightsMap.get(light.getPlace()).add(light);
    }

    public List<Light> lookupLights(Place place) {
        if (!lightsMap.containsKey(place)) {
            return new LinkedList<>();
        } else {
            return lightsMap.get(place);
        }
    }

    public Map<Place, List<Light>> getLightsMap() {
        return lightsMap;
    }

    public void setLightsMap(Map<Place, List<Light>> lightsMap) {
        this.lightsMap = lightsMap;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
