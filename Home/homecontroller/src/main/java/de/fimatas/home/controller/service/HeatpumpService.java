package de.fimatas.home.controller.service;

import com.fasterxml.jackson.databind.JsonNode;
import de.fimatas.heatpumpdriver.api.*;
import de.fimatas.home.controller.api.HueAPI;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

@Component
@CommonsLog
public class HeatpumpService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment env;

    private boolean isCallError = false; // prevent continous error calls

    private Map<Place, String> dictPlaceToRoomNameInDriver;

    private static final Log LOG = LogFactory.getLog(HeatpumpService.class);

    @PostConstruct
    public void init() {

        dictPlaceToRoomNameInDriver = Map.of( //
                Place.BEDROOM, Place.BEDROOM.getPlaceName(), //
                Place.KIDSROOM_1, env.getProperty("place.KIDSROOM_1.subtitle"), //
                Place.KIDSROOM_2, env.getProperty("place.KIDSROOM_2.subtitle") //
        );

        CompletableFuture.runAsync(() -> {
            try {
                refreshHeatpumpModel(true);
            } catch (Exception e) {
                LOG.error("Could not initialize HeatpumpService completly.", e);
            }
        });
    }

    @Scheduled(fixedDelay = (1000 * HomeAppConstants.MODEL_DEFAULT_INTERVAL_SECONDS) + 400)
    public void scheduledRefreshFromDriverCache() {
        refreshHeatpumpModel(true);
    }

    @Scheduled(cron = "3 30 00 * * *")
    private void scheduledRefreshFromDriverNoCache() {
        isCallError = false;
        refreshHeatpumpModel(false);
    }

    private synchronized void refreshHeatpumpModel(boolean cachedData) {

        if(!cachedData && isCallError){
            return;
        }

        HeatpumpRequest request = HeatpumpRequest.builder()
                .readWithRoomnames(new ArrayList(dictPlaceToRoomNameInDriver.values()))
                .heatpumpUsername("aaa") // FIXME
                .heatpumpPassword("bbb") // FIXME
                .readFromCache(cachedData)
                .build();

        HeatpumpResponse response = callDriver(request);
        if(cachedData && response.isCacheNotPresentError() && !isCallError){
            // Fallback from cached to direct call
            log.warn("heatpump cache empty, calling again without cache");
            request.setReadFromCache(false);
            response = callDriver(request);
            isCallError = !response.isDriverRunSuccessful() || StringUtils.isNotBlank(response.getErrorMessage());
        }

        handleResponse(response);
    }

    private void handleResponse(HeatpumpResponse response) {

        if(!response.isRemoteConnectionSuccessful() || !response.isDriverRunSuccessful() || StringUtils.isNotBlank(response.getErrorMessage())){
            log.warn("Error calling heatpump driver: " + response.getErrorMessage());
            // fixme: handle/upload error model
            return;
        }

        HeatpumpModel newModel = new HeatpumpModel();
        newModel.setTimestamp(System.currentTimeMillis());

        response.getRoomnamesAndStates().forEach((r, s) -> {

            Place place = dictPlaceToRoomNameInDriver.entrySet().stream().filter(e ->
                    e.getValue().equalsIgnoreCase(r)).findFirst().orElseThrow().getKey();

            Heatpump heatpump = new Heatpump();
            heatpump.setPlace(place);

            if(!s.getOnOffSwitch().booleanValue()){
                heatpump.setHeatpumpPreset(HeatpumpPreset.OFF);
            }else if(s.getMode() == HeatpumpMode.COOLING){
                if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                    heatpump.setHeatpumpPreset(HeatpumpPreset.COOL_AUTO);
                }else{
                    heatpump.setHeatpumpPreset(HeatpumpPreset.COOL_MIN);
                }
            }else if(s.getMode() == HeatpumpMode.HEATING){
                if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                    heatpump.setHeatpumpPreset(HeatpumpPreset.HEAT_AUTO);
                }else{
                    heatpump.setHeatpumpPreset(HeatpumpPreset.HEAT_MIN);
                }
            }else if(s.getMode() == HeatpumpMode.FAN){
                // FIXME: HANDLE DRY-TIMER
                if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                    heatpump.setHeatpumpPreset(HeatpumpPreset.FAN_AUTO);
                }else{
                    heatpump.setHeatpumpPreset(HeatpumpPreset.FAN_MIN);
                }
            }else{
                heatpump.setHeatpumpPreset(HeatpumpPreset.UNKNOWN);
            }

            newModel.getHeatpumpMap().put(place, heatpump);
        });

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    public void preset(Place place, HeatpumpPreset preset, String additionalData) {

        // FIXME: OTHER MODES ON OTHER ACTIVE ROOMS !

        log.debug("HEATPUMP PRESET CALL: " + place.name() + ": " + preset.name() + " WITH: " + additionalData);

        switchModelToBusy();

        CompletableFuture.runAsync(() -> {
            Map<String, HeatpumpProgram> programs = new HashMap<>();
            programs.put(dictPlaceToRoomNameInDriver.get(place), presetToProgram(preset));
            List.of(StringUtils.split(additionalData, ',')).
                    forEach(ap -> programs.put(dictPlaceToRoomNameInDriver.get(ap), presetToProgram(preset)));

            HeatpumpRequest request = HeatpumpRequest.builder()
                    .writeWithRoomnameAndProgram(programs)
                    .readWithRoomnames(new ArrayList(dictPlaceToRoomNameInDriver.values()))
                    .heatpumpUsername("aaa") // FIXME
                    .heatpumpPassword("bbb") // FIXME
                    .readFromCache(false)
                    .build();

            handleResponse(callDriver(request));
        });
    }

    private void switchModelToBusy() {

        final HeatpumpModel busyHeatpumpModel = ModelObjectDAO.getInstance().readHeatpumpModel();
        busyHeatpumpModel.setBusy(true);
        busyHeatpumpModel.setTimestamp(System.currentTimeMillis());
        ModelObjectDAO.getInstance().write(busyHeatpumpModel);
        uploadService.uploadToClient(busyHeatpumpModel);
    }

    private HeatpumpProgram presetToProgram(HeatpumpPreset preset){
        switch (preset){
            case COOL_AUTO:
                return HeatpumpProgram.COOLING_AUTO;
            case COOL_MIN:
                return HeatpumpProgram.COOLING_MIN;
            case HEAT_AUTO:
                return HeatpumpProgram.HEATING_AUTO; // FIXME: write program
            case HEAT_MIN:
                return HeatpumpProgram.HEATING_MIN; // FIXME: write program
            case FAN_AUTO:
                return HeatpumpProgram.FAN_AUTO; // FIXME: write program
            case FAN_MIN:
                return HeatpumpProgram.FAN_MIN;
            case DRY_TIMER:
                // TODO
                return HeatpumpProgram.FAN_MIN;
            case OFF:
                return HeatpumpProgram.OFF;
            default:
                throw new IllegalArgumentException("unknown preset: " + preset);
        }
    }

    private synchronized HeatpumpResponse callDriver(HeatpumpRequest request){

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-cache");

        try {
            HttpEntity<HeatpumpRequest> httpRequest = new HttpEntity<>(request);
            // FIXME: URL
            ResponseEntity<HeatpumpResponse> response = restTemplate.postForEntity("http://localhost:8090/callHeatpumpMock", httpRequest, HeatpumpResponse.class);
            HttpStatus statusCode = response.getStatusCode();

            if (!statusCode.is2xxSuccessful()) {
                log.error("Could not call heatpump driver. RC=" + statusCode.value());
                isCallError = true;
            }
            final HeatpumpResponse body = response.getBody();
            body.setRemoteConnectionSuccessful(true);
            return body;

        } catch (ResourceAccessException | HttpServerErrorException | HttpClientErrorException e) {
            log.error("Exception calling heatpump driver:" + e.getMessage());
            isCallError = true;
            var response = new HeatpumpResponse();
            response.setRemoteConnectionSuccessful(false);
            response.setErrorMessage("Exception calling heatpump driver:" + e.getMessage());
            return response;
        }
    }

}
