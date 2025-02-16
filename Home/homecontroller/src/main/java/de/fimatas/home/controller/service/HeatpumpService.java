package de.fimatas.home.controller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.heatpumpdriver.api.*;
import de.fimatas.home.controller.api.ExternalServiceHttpAPI;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Component
@CommonsLog
public class HeatpumpService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private PushService pushService;

    @Autowired
    private ExternalServiceHttpAPI externalServiceHttpAPI;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private Environment env;

    @Value("${application.externalServicesEnabled:false}")
    private boolean externalServicesEnabled;

    @Value("${application.heatpumpRefreshEnabled:false}")
    private boolean heatpumpRefreshEnabled;

    private boolean isCallError = false; // prevent continous error calls

    private Map<Place, String> dictPlaceToRoomNameInDriver;

    private Map<Place, Optional<SchedulerData>> placeScheduler;

    private final Map<HeatpumpPreset, SchedulerConfig> schedulerConfigMap = Map.of(
            HeatpumpPreset.DRY_TIMER, new SchedulerConfig(HeatpumpPreset.OFF, HeatpumpPreset.FAN_MIN, 20),
            HeatpumpPreset.HEAT_TIMER1, new SchedulerConfig(HeatpumpPreset.OFF, HeatpumpPreset.HEAT_MIN, 60),
            HeatpumpPreset.HEAT_TIMER2, new SchedulerConfig(HeatpumpPreset.OFF, HeatpumpPreset.HEAT_MIN, 120),
            HeatpumpPreset.COOL_TIMER1, new SchedulerConfig(HeatpumpPreset.DRY_TIMER, HeatpumpPreset.COOL_MIN, 60),
            HeatpumpPreset.COOL_TIMER2, new SchedulerConfig(HeatpumpPreset.DRY_TIMER, HeatpumpPreset.COOL_MIN, 120)
            );

    private final Map<HeatpumpPreset, List<HeatpumpPreset>> conflictiongPresets = Map.of(
            HeatpumpPreset.COOL_MIN, List.of(HeatpumpPreset.HEAT_AUTO, HeatpumpPreset.HEAT_MIN, HeatpumpPreset.HEAT_TIMER1, HeatpumpPreset.HEAT_TIMER2),
            HeatpumpPreset.COOL_AUTO, List.of(HeatpumpPreset.HEAT_AUTO, HeatpumpPreset.HEAT_MIN, HeatpumpPreset.HEAT_TIMER1, HeatpumpPreset.HEAT_TIMER2),
            HeatpumpPreset.HEAT_MIN, List.of(HeatpumpPreset.COOL_AUTO, HeatpumpPreset.COOL_MIN, HeatpumpPreset.COOL_TIMER1, HeatpumpPreset.COOL_TIMER2),
            HeatpumpPreset.HEAT_AUTO, List.of(HeatpumpPreset.COOL_AUTO, HeatpumpPreset.COOL_MIN, HeatpumpPreset.COOL_TIMER1, HeatpumpPreset.COOL_TIMER2)
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

    @Scheduled(cron = "50 4/10 * * * *")
    public void scheduledRefreshFromDriverCache() {
        if(!isRestartInTimerangeMinutes(10)) {
            refreshHeatpumpModel(true);
        }
    }

    @Scheduled(cron = "42 15 13,20 * * *")
    public void scheduledRefreshFromDriverNoCache() {
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
            final LocalDateTime previous = cronExpression.next(LocalDateTime.now().minusDays(1));
            return Math.abs(ChronoUnit.MINUTES.between(Objects.requireNonNull(next), LocalDateTime.now())) <= minutes
                    || Math.abs(ChronoUnit.MINUTES.between(Objects.requireNonNull(previous), LocalDateTime.now())) <= minutes ;
        }
        return false;
    }

    private synchronized void refreshHeatpumpModel(boolean cachedData) {

        if(!externalServicesEnabled || !heatpumpRefreshEnabled){
            return;
        }

        if(!cachedData && isCallError){
            return;
        }

        HeatpumpRequest request = new HeatpumpRequest();
        request.getReadWithRoomnames().addAll(dictPlaceToRoomNameInDriver.values());
        request.setHeatpumpUsername(env.getProperty("heatpump.driver.user"));
        request.setHeatpumpPassword(env.getProperty("heatpump.driver.pass"));
        request.setReadFromCache(cachedData);

        HeatpumpResponse response = callDriver(request);
        if(cachedData && response.isCacheNotPresentError() && !isCallError){
            // Fallback from cached to direct call
            log.info("heatpump cache empty, calling again without cache");
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
                newModel.getHeatpumpMap().put(place, new Heatpump(
                        place, placeScheduler.get(place).get().getSchedulerPreset(), placeScheduler.get(place).get().getScheduledTimeInstant().toEpochMilli()));
            }else{
                newModel.getHeatpumpMap().put(place, new Heatpump(place, preset, null));
            }
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

    public void timerPreset(List<Place> places, HeatpumpPreset preset, Instant scheduledTime) {

        final List<Place> validTimerPlaces =
                places.stream().filter(p -> placeScheduler.get(p).isPresent() && placeScheduler.get(p).get().getScheduledTimeInstant() == scheduledTime).collect(Collectors.toList());

        preset(validTimerPlaces, preset);
    }

    public void preset(List<Place> places, HeatpumpPreset preset) {

        if(places.isEmpty() || ModelObjectDAO.getInstance().readHeatpumpModel() == null){
            return;
        }

        switchModelToBusy();

        cancelOldTimers(places);

        CompletableFuture.runAsync(() -> startPresetInternal(places, preset));
    }

    private synchronized void startPresetInternal(List<Place> places, HeatpumpPreset preset) {

        Map<String, HeatpumpProgram> programs = new HashMap<>();
        places.forEach(p -> {
            final HeatpumpProgram program = presetToProgram(preset);
            final Map<Place, Heatpump> heatpumpMap = ModelObjectDAO.getInstance().readHeatpumpModel().getHeatpumpMap();
            if (program != null && heatpumpMap.containsKey(p) && presetToProgram(heatpumpMap.get(p).getHeatpumpPreset()) != program) {
                programs.put(dictPlaceToRoomNameInDriver.get(p), program);
            }
        });

        if(conflictiongPresets.containsKey(preset)){
            dictPlaceToRoomNameInDriver.keySet().stream().filter(p -> !places.contains(p)).forEach(px -> {
                if(conflictiongPresets.get(preset).contains(
                        ModelObjectDAO.getInstance().readHeatpumpModel().getHeatpumpMap().get(px).getHeatpumpPreset())){
                    programs.put(dictPlaceToRoomNameInDriver.get(px), HeatpumpProgram.OFF);
                }
            });
        }

        HeatpumpRequest request = new HeatpumpRequest();
        request.getReadWithRoomnames().addAll(dictPlaceToRoomNameInDriver.values());
        request.getWriteWithRoomnameAndProgram().putAll(programs);
        request.setHeatpumpUsername(env.getProperty("heatpump.driver.user"));
        request.setHeatpumpPassword(env.getProperty("heatpump.driver.pass"));
        request.setReadFromCache(false);

        final HeatpumpResponse response = callDriver(request);
        if(!responseHasError(response)){
            if(areExpectedModesSet(places, presetToProgram(preset), response)){
                scheduleNewTimers(places, preset);
            }else{
                CompletableFuture.runAsync(() -> pushService.sendErrorMessage("Wärmepumpe befindet sich nicht im erwarteten Modus!"));
            }
        }

        handleResponse(request, response);
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
                // do not cancel timer -let it run for other places!
                placeScheduler.put(p, Optional.empty());
            }
        });
    }

    private void scheduleNewTimers(List<Place> places, HeatpumpPreset preset) {

        if(schedulerConfigMap.containsKey(preset)){
            var config = schedulerConfigMap.get(preset);
            var scheduledTime = Instant.now().plus(config.getTimeInMinutes(), ChronoUnit.MINUTES);
            final ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.schedule(() ->
                    timerPreset(places, config.getTarget(), scheduledTime), scheduledTime);
            places.forEach(p -> placeScheduler.put(p, Optional.of(new SchedulerData(scheduledFuture, preset, config.getBase(), scheduledTime))));
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
                place -> unknownHeatpumpModel.getHeatpumpMap().put(place, new Heatpump(place, HeatpumpPreset.UNKNOWN, null)));
        return unknownHeatpumpModel;
    }

    private HeatpumpProgram presetToProgram(HeatpumpPreset preset){
        return switch (preset) {
            case COOL_AUTO -> HeatpumpProgram.COOLING_AUTO;
            case COOL_MIN, COOL_TIMER1, COOL_TIMER2 -> HeatpumpProgram.COOLING_MIN;
            case HEAT_AUTO -> HeatpumpProgram.HEATING_AUTO;
            case HEAT_MIN, HEAT_TIMER1, HEAT_TIMER2 -> HeatpumpProgram.HEATING_MIN;
            case FAN_AUTO -> HeatpumpProgram.FAN_AUTO;
            case FAN_MIN -> HeatpumpProgram.FAN_MIN;
            case DRY_TIMER -> HeatpumpProgram.FAN_DRY;
            case OFF -> HeatpumpProgram.OFF;
            case UNKNOWN -> null; // refresh only
        };
    }

    private synchronized HeatpumpResponse callDriver(HeatpumpRequest request){

        try {
            ResponseEntity<HeatpumpResponse> response = externalServiceHttpAPI.postForHeatpumpEntity(
                    Objects.requireNonNull(env.getProperty("heatpump.driver.url")), request);
            HttpStatusCode statusCode = response.getStatusCode();

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
        private Instant scheduledTimeInstant;
    }

    @Data
    @AllArgsConstructor
    private static class SchedulerConfig{
        HeatpumpPreset target;
        HeatpumpPreset base;
        Integer timeInMinutes;
    }
}
