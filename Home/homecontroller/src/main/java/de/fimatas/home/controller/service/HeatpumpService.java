package de.fimatas.home.controller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.heatpumpdriver.api.*;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
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
    private PushService pushService;

    @Autowired
    @Qualifier("restTemplateHeatpumpDriver")
    private RestTemplate restTemplateHeatpumpDriver;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private Environment env;

    private boolean isCallError = false; // prevent continous error calls

    private Map<Place, String> dictPlaceToRoomNameInDriver;

    private Map<Place, Optional<SchedulerData>> placeScheduler;

    private final Map<HeatpumpPreset, List<HeatpumpPreset>> conflictiongPresets = Map.of(
            HeatpumpPreset.COOL_MIN, List.of(HeatpumpPreset.HEAT_AUTO, HeatpumpPreset.HEAT_MIN),
            HeatpumpPreset.COOL_AUTO, List.of(HeatpumpPreset.HEAT_AUTO, HeatpumpPreset.HEAT_MIN),
            HeatpumpPreset.HEAT_MIN, List.of(HeatpumpPreset.COOL_AUTO, HeatpumpPreset.COOL_MIN),
            HeatpumpPreset.HEAT_AUTO, List.of(HeatpumpPreset.COOL_AUTO, HeatpumpPreset.COOL_MIN)
            );

    @PostConstruct
    public void init() {

        dictPlaceToRoomNameInDriver = Map.of( //
                Place.BEDROOM, Place.BEDROOM.getPlaceName(), //
                Place.KIDSROOM_1, Objects.requireNonNull(env.getProperty("place.KIDSROOM_1.subtitle")), //
                Place.KIDSROOM_2, Objects.requireNonNull(env.getProperty("place.KIDSROOM_2.subtitle")) //
        );

        placeScheduler = new EnumMap<>(Place.class);
        dictPlaceToRoomNameInDriver.keySet().forEach(place -> placeScheduler.put(place, Optional.empty()));
    }

    @Scheduled(initialDelay = 1000 * 10, fixedDelay = (1000 * HomeAppConstants.MODEL_HEATPUMP_INTERVAL_SECONDS) + 180)
    public void scheduledRefreshFromDriverCache() {
        if(!isRestartInTimerangeMinutes(10)) {
            refreshHeatpumpModel(true);
        }
    }

    @Scheduled(cron = "42 15 4,13 * * *")
    private void scheduledRefreshFromDriverNoCache() {
        isCallError = false;
        if(isRestartInTimerangeMinutes(60 * 3)) {
            // no non-cache call if server restarts within +- three hours (and resets cache)
            return;
        }
        refreshHeatpumpModel(false);
    }

    private boolean isRestartInTimerangeMinutes(int minutes){
        String serverRestartCron = env.getProperty("heatpump.server.restartCron");
        if(StringUtils.isNotBlank(serverRestartCron)) {
            CronExpression cronExpression = CronExpression.parse(serverRestartCron);
            final LocalDateTime next = cronExpression.next(LocalDateTime.now());
            final LocalDateTime previous = cronExpression.next(LocalDateTime.now().minus(1, ChronoUnit.DAYS));
            return Math.abs(ChronoUnit.MINUTES.between(next, LocalDateTime.now())) <= minutes
                    || Math.abs(ChronoUnit.MINUTES.between(previous, LocalDateTime.now())) <= minutes ;
        }
        return false;
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

        handleResponse(request, response);
    }

    private void handleResponse(HeatpumpRequest request, HeatpumpResponse response) {

        if(responseHasError(response)){
            try {
                log.warn("Error calling heatpump driver: " + new ObjectMapper().writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("Error calling heatpump driver....");
            }
            if(!request.isReadFromCache() || (request.getWriteWithRoomnameAndProgram() != null && !request.getWriteWithRoomnameAndProgram().isEmpty())){
                CompletableFuture.runAsync(() -> pushService.sendErrorMessage("Fehler bei Ansteuerung der Wärmepumpe!"));
            }
            switchModelToUnknown();
            return;
        }

        HeatpumpModel newModel = getHeatpumpModelWithUnknownPresets();
        response.getRoomnamesAndStates().forEach((r, s) -> {

            Place place = dictPlaceToRoomNameInDriver.entrySet().stream().filter(e ->
                    e.getValue().equalsIgnoreCase(r)).findFirst().orElseThrow().getKey();

            HeatpumpPreset preset = lookupPresetFromModeAndSpeed(s);

            // upgrade to scheduled preset
            if(placeScheduler.get(place).isPresent() && placeScheduler.get(place).get().getBasePreset() == preset) {
                preset = placeScheduler.get(place).get().getSchedulerPreset();
            }

            newModel.getHeatpumpMap().put(place, new Heatpump(place, preset));
        });

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    private HeatpumpPreset lookupPresetFromModeAndSpeed(HeatpumpState s) {

        if(!s.getOnOffSwitch()){
            return HeatpumpPreset.OFF;
        }else if(s.getMode() == HeatpumpMode.COOLING){
            if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                return HeatpumpPreset.COOL_AUTO;
            }else{
                return HeatpumpPreset.COOL_MIN;
            }
        }else if(s.getMode() == HeatpumpMode.HEATING){
            if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                return HeatpumpPreset.HEAT_AUTO;
            }else{
                return HeatpumpPreset.HEAT_MIN;
            }
        }else if(s.getMode() == HeatpumpMode.FAN){
            if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                return HeatpumpPreset.FAN_AUTO;
            }else{
                return HeatpumpPreset.FAN_MIN;
            }
        }else{
            return HeatpumpPreset.UNKNOWN;
        }
    }

    private boolean responseHasError(HeatpumpResponse response) {
        return !response.isRemoteConnectionSuccessful() || !response.isDriverRunSuccessful() || StringUtils.isNotBlank(response.getErrorMessage());
    }

    public void preset(Place place, HeatpumpPreset preset, String additionalData) {

        switchModelToBusy();

        List<Place> allPlaces = new LinkedList<>();
        allPlaces.add(place);
        List.of(StringUtils.split(additionalData, ',')).forEach(ap ->allPlaces.add(Place.valueOf(ap)));

        cancelOldTimers(allPlaces);

        CompletableFuture.runAsync(() -> {
            Map<String, HeatpumpProgram> programs = new HashMap<>();
            allPlaces.forEach(p -> {
                final HeatpumpProgram program = presetToProgram(preset);
                if (program != null) {
                    programs.put(dictPlaceToRoomNameInDriver.get(p), program);
                }
            });

            if(conflictiongPresets.containsKey(preset)){
                dictPlaceToRoomNameInDriver.keySet().stream().filter(p -> !allPlaces.contains(p)).forEach(px -> {
                    if(conflictiongPresets.get(preset).contains(
                            ModelObjectDAO.getInstance().readHeatpumpModel().getHeatpumpMap().get(px).getHeatpumpPreset())){
                        programs.put(dictPlaceToRoomNameInDriver.get(px), HeatpumpProgram.OFF);
                    }
                });
            }

            HeatpumpRequest request = HeatpumpRequest.builder()
                    .writeWithRoomnameAndProgram(programs)
                    .readWithRoomnames(new ArrayList<>(dictPlaceToRoomNameInDriver.values()))
                    .heatpumpUsername(env.getProperty("heatpump.driver.user"))
                    .heatpumpPassword(env.getProperty("heatpump.driver.pass"))
                    .readFromCache(false)
                    .build();

            final HeatpumpResponse response = callDriver(request);
            if(!responseHasError(response)){
                if(areExpectedModesSet(allPlaces, presetToProgram(preset), response)){
                    scheduleNewTimers(place, preset, additionalData, allPlaces);
                }else{
                    CompletableFuture.runAsync(() -> pushService.sendErrorMessage("Wärmepumpe befindet sich nicht im erwarteten Modus!"));
                }
            }

            handleResponse(request, response);
        });
    }

    private boolean areExpectedModesSet(List<Place> allPlaces, HeatpumpProgram program, HeatpumpResponse response) {

        if(program==null){
            return true;
        }

        for(Map.Entry<String, HeatpumpState> entry : response.getRoomnamesAndStates().entrySet()){

            Place place = dictPlaceToRoomNameInDriver.entrySet().stream().filter(e ->
                    e.getValue().equalsIgnoreCase(entry.getKey())).findFirst().orElseThrow().getKey();

            if(allPlaces.contains(place)){
                if(program.isExpectedOnOffState() != entry.getValue().getOnOffSwitch()){
                    return false;
                }else if(entry.getValue().getOnOffSwitch() && (entry.getValue().getMode() != program.getExpectedMode())){
                    return false;
                }
            }
        }
        return true;
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
            final ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.schedule(() ->
                    preset(place, HeatpumpPreset.OFF, additionalData), Instant.now().plus(15, ChronoUnit.MINUTES));
            allPlaces.forEach(p -> placeScheduler.put(p, Optional.of(new SchedulerData(scheduledFuture, preset, HeatpumpPreset.FAN_MIN))));
        }
    }

    private void switchModelToBusy() {

        final HeatpumpModel busyHeatpumpModel = ModelObjectDAO.getInstance().readHeatpumpModel();
        busyHeatpumpModel.setBusy(true);
        busyHeatpumpModel.setTimestamp(System.currentTimeMillis());
        ModelObjectDAO.getInstance().write(busyHeatpumpModel);
        uploadService.uploadToClient(busyHeatpumpModel);
    }

    private void switchModelToUnknown() {

        final HeatpumpModel unknownHeatpumpModel = getHeatpumpModelWithUnknownPresets();
        ModelObjectDAO.getInstance().write(unknownHeatpumpModel);
        uploadService.uploadToClient(unknownHeatpumpModel);
    }

    private HeatpumpModel getHeatpumpModelWithUnknownPresets() {

        final var unknownHeatpumpModel = new HeatpumpModel();
        unknownHeatpumpModel.setTimestamp(System.currentTimeMillis());
        dictPlaceToRoomNameInDriver.keySet().forEach(
                place -> unknownHeatpumpModel.getHeatpumpMap().put(place, new Heatpump(place, HeatpumpPreset.UNKNOWN)));
        return unknownHeatpumpModel;
    }

    private HeatpumpProgram presetToProgram(HeatpumpPreset preset){
        switch (preset){
            case COOL_AUTO:
                return HeatpumpProgram.COOLING_AUTO;
            case COOL_MIN:
                return HeatpumpProgram.COOLING_MIN;
            case HEAT_AUTO:
                return HeatpumpProgram.HEATING_AUTO;
            case HEAT_MIN:
                return HeatpumpProgram.HEATING_MIN;
            case FAN_AUTO:
                return HeatpumpProgram.FAN_AUTO;
            case FAN_MIN:
                return HeatpumpProgram.FAN_MIN;
            case DRY_TIMER:
                return HeatpumpProgram.FAN_DRY;
            case OFF:
                return HeatpumpProgram.OFF;
            case UNKNOWN:
                return null; // refresh only
            default:
                throw new IllegalArgumentException("unknown preset: " + preset);
        }
    }

    private synchronized HeatpumpResponse callDriver(HeatpumpRequest request){

        try {
            HttpEntity<HeatpumpRequest> httpRequest = new HttpEntity<>(request);
            ResponseEntity<HeatpumpResponse> response = restTemplateHeatpumpDriver.postForEntity(
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
        private HeatpumpPreset schedulerPreset;
        private HeatpumpPreset basePreset;
    }

}
