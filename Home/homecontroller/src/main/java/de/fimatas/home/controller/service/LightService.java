package de.fimatas.home.controller.service;

import com.fasterxml.jackson.databind.JsonNode;
import de.fimatas.home.controller.api.HueAPI;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.Light;
import de.fimatas.home.library.domain.model.LightState;
import de.fimatas.home.library.domain.model.LightsModel;
import de.fimatas.home.library.domain.model.Place;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map.Entry;

@Component
public class LightService {

    @Autowired
    private HueAPI hueAPI;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private HouseService houseService;

    private Map<List<String>, Place> placesToNameAndSubtitle;

    private static final String JSON_PATH_REACHABLE = "reachable";

    private static final String JSON_PATH_GROUPS = "groups";

    private static final String JSON_PATH_ON = "on";

    private static final String JSON_PATH_STATE = "state";

    private static final String JSON_PATH_LIGHTS = "lights";

    private static final String JSON_PATH_NAME = "name";

    private static final Log LOG = LogFactory.getLog(LightService.class);

    @PostConstruct
    public void init() {
        prepareSubtitleMap();
    }

    private void prepareSubtitleMap(){
        placesToNameAndSubtitle = new LinkedHashMap<>();
        Arrays.stream(Place.values()).forEach(
                p -> houseService.readSubtitleFor(p).ifPresent(s -> placesToNameAndSubtitle.put(List.of(p.getPlaceName(), s), p)));
    }

    @Scheduled(cron = "7/10 * * * * *")
    public void scheduledRefreshLightModel() {
        refreshLightsModel();
    }

    public void refreshLightsModel() {

        LightsModel newModel = refreshModel();
        if (newModel == null) {
            return;
        }

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    private LightsModel refreshModel() {

        try {
            if (!hueAPI.refresh()) {
                return null;
            }
        } catch (Exception e) {
            LOG.error("error processing hue response", e);
            return null;
        }

        if (hueAPI.getRootNode() == null) {
            return null;
        }

        JsonNode groups = hueAPI.getRootNode().path(JSON_PATH_GROUPS);
        JsonNode allLights = hueAPI.getRootNode().path(JSON_PATH_LIGHTS);

        LightsModel model = new LightsModel();

        groups.properties().forEach(group -> mapGroup(group, allLights, model));

        return model;
    }

    private void mapGroup(Entry<String, JsonNode> group, JsonNode allLights, LightsModel model) {

        String groupName = group.getValue().path(JSON_PATH_NAME).asText();

        JsonNode groupLights = group.getValue().get(JSON_PATH_LIGHTS);
        for (JsonNode groupLight : groupLights) {
            Place place;
            if ((place = Place.fromName(groupName))!= null) {
                model.addLight(mapLight(allLights, groupLight.asText(), place));
            } else {
                List<String> token = List.of(StringUtils.split(groupName, ' '));
                placesToNameAndSubtitle.entrySet().stream().filter(e -> new HashSet<>(e.getKey()).containsAll(token)).
                        findAny().ifPresent(e -> model.addLight(mapLight(allLights, groupLight.asText(), e.getValue())));
            }
        }

    }

    public Light mapLight(JsonNode allLights, String lightId, Place place) {

        JsonNode light = allLights.path(lightId);
        String lightName = light.path(JSON_PATH_NAME).asText();
        JsonNode lightState = light.path(JSON_PATH_STATE);
        boolean lightOn = lightState.path(JSON_PATH_ON).asBoolean();
        boolean lightReachable = lightState.path(JSON_PATH_REACHABLE).asBoolean();

        Light lightModel = new Light();
        lightModel.setName(lightName);
        lightModel.setId(lightId);
        lightModel.setPlace(place);
        lightModel.setState(mapLightState(lightOn, lightReachable));

        return lightModel;
    }

    public LightState mapLightState(boolean lightOn, boolean lightReachable) {

        if (!lightReachable) {
            return LightState.SWITCH_OFF;
        } else {
            if (lightOn) {
                return LightState.ON;
            } else {
                return LightState.OFF;
            }
        }
    }

    public void toggleLight(String deviceId, Boolean value) {
        hueAPI.toggleLight(deviceId, value);
    }

}
