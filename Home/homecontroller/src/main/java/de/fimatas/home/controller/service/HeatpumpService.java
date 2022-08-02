package de.fimatas.home.controller.service;

import de.fimatas.heatpumpdriver.api.*;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

@Component
@CommonsLog
public class HeatpumpService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskSchedulerHeatpumpTimer;

    @Autowired
    private Environment env;

    private boolean isCallError = false; // prevent continous error calls

    private Map<Place, String> dictPlaceToRoomNameInDriver;

    private Map<Place, Optional<SchedulerData>> placeScheduler;

    private static final Log LOG = LogFactory.getLog(HeatpumpService.class);

    @PostConstruct
    public void init() {

        dictPlaceToRoomNameInDriver = Map.of( //
                Place.BEDROOM, Place.BEDROOM.getPlaceName(), //
                Place.KIDSROOM_1, Objects.requireNonNull(env.getProperty("place.KIDSROOM_1.subtitle")), //
                Place.KIDSROOM_2, Objects.requireNonNull(env.getProperty("place.KIDSROOM_2.subtitle")) //
        );

        placeScheduler = new EnumMap<>(Place.class);
        dictPlaceToRoomNameInDriver.keySet().forEach(place -> placeScheduler.put(place, Optional.empty()));

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
                .readWithRoomnames(new ArrayList<>(dictPlaceToRoomNameInDriver.values()))
                .heatpumpUsername(env.getProperty("heatpump.driver.user"))
                .heatpumpPassword(env.getProperty("heatpump.driver.pass"))
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

            if(placeScheduler.get(place).isPresent()){
                heatpump.setHeatpumpPreset(placeScheduler.get(place).get().getHeatpumpPreset());
            }else if(!s.getOnOffSwitch()){
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

        List<Place> allPlaces = new LinkedList<>();
        allPlaces.add(place);
        List.of(StringUtils.split(additionalData, ',')).forEach(ap ->allPlaces.add(Place.valueOf(ap)));

        cancelOldTimers(allPlaces);
        scheduleNewTimers(place, preset, additionalData, allPlaces);

        CompletableFuture.runAsync(() -> {
            Map<String, HeatpumpProgram> programs = new HashMap<>();
            allPlaces.forEach(p -> programs.put(dictPlaceToRoomNameInDriver.get(p), presetToProgram(preset)));

            HeatpumpRequest request = HeatpumpRequest.builder()
                    .writeWithRoomnameAndProgram(programs)
                    .readWithRoomnames(new ArrayList<>(dictPlaceToRoomNameInDriver.values()))
                    .heatpumpUsername(env.getProperty("heatpump.driver.user"))
                    .heatpumpPassword(env.getProperty("heatpump.driver.pass"))
                    .readFromCache(false)
                    .build();

            handleResponse(callDriver(request));
        });
    }

    private void cancelOldTimers(List<Place> allPlaces) {

        allPlaces.forEach(p -> {
            if(placeScheduler.get(p).isPresent()){
                if(!placeScheduler.get(p).get().getScheduledFuture().isCancelled() && !placeScheduler.get(p).get().getScheduledFuture().isDone()){
                    placeScheduler.get(p).get().getScheduledFuture().cancel(false);
                }
                placeScheduler.put(p, Optional.empty());
            }
        });
    }

    private void scheduleNewTimers(Place place, HeatpumpPreset preset, String additionalData, List<Place> allPlaces) {

        if(preset == HeatpumpPreset.DRY_TIMER){
            final ScheduledFuture<?> scheduledFuture = threadPoolTaskSchedulerHeatpumpTimer.schedule(() ->
                    preset(place, HeatpumpPreset.OFF, additionalData), Instant.now().plus(1, ChronoUnit.MINUTES));
            allPlaces.forEach(p -> placeScheduler.put(p, Optional.of(new SchedulerData(scheduledFuture, preset))));
        }
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
            case DRY_TIMER:
                return HeatpumpProgram.FAN_MIN;
            case OFF:
                return HeatpumpProgram.OFF;
            default:
                throw new IllegalArgumentException("unknown preset: " + preset);
        }
    }

    private synchronized HeatpumpResponse callDriver(HeatpumpRequest request){

        try {
            HttpEntity<HeatpumpRequest> httpRequest = new HttpEntity<>(request);
            ResponseEntity<HeatpumpResponse> response = restTemplate.postForEntity(
                    Objects.requireNonNull(env.getProperty("heatpump.driver.url")), httpRequest, HeatpumpResponse.class);
            HttpStatus statusCode = response.getStatusCode();

            if (!statusCode.is2xxSuccessful()) {
                log.error("Could not call heatpump driver. RC=" + statusCode.value());
                isCallError = true;
            }
            final HeatpumpResponse body = response.getBody();
            if(body != null){
                body.setRemoteConnectionSuccessful(true);
            }
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

    @Data
    @AllArgsConstructor
    private static class SchedulerData{
        private ScheduledFuture<?> scheduledFuture;
        private HeatpumpPreset heatpumpPreset;
    }

}
