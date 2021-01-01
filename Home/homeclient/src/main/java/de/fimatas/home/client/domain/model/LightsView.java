package de.fimatas.home.client.domain.model;

import java.util.LinkedList;
import java.util.List;

public class LightsView extends View {

    private List<LightView> lights = new LinkedList<>();

    public List<LightView> getLights() {
        return lights;
    }

    public void setLights(List<LightView> lights) {
        this.lights = lights;
    }
}
