package de.fimatas.home.controller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fimatas.heatpump.roof.driver.api.*;
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

import jakarta.annotation.PostConstruct;
import org.springframework.web.client.RestClientException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Component
@CommonsLog
public class HeatpumpRoofService {

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

    @Value("${heatpump.roof.name:UnbekannteWaermepumpe}")
    private String heatpumpName;

    private boolean isCallError = false; // prevent continous error calls

    private Map<Place, String> dictPlaceToRoomNameInDriver;

    private Map<Place, Optional<HeatpumpRoofService.SchedulerData>> placeScheduler;

    private final Map<HeatpumpRoofPreset, HeatpumpRoofService.SchedulerConfig> schedulerConfigMap = Map.of(
            HeatpumpRoofPreset.DRY_TIMER, new HeatpumpRoofService.SchedulerConfig(HeatpumpRoofPreset.OFF, HeatpumpRoofPreset.FAN_MIN, 20, null),
            HeatpumpRoofPreset.HEAT_TIMER1, new HeatpumpRoofService.SchedulerConfig(HeatpumpRoofPreset.OFF, HeatpumpRoofPreset.HEAT_AUTO, 60, null),
            HeatpumpRoofPreset.HEAT_TIMER2, new HeatpumpRoofService.SchedulerConfig(HeatpumpRoofPreset.OFF, HeatpumpRoofPreset.HEAT_AUTO, 120, null),
            HeatpumpRoofPreset.HEAT_TIMER3, new HeatpumpRoofService.SchedulerConfig(HeatpumpRoofPreset.OFF, HeatpumpRoofPreset.HEAT_AUTO, null, LocalTime.of(13,0)),
            HeatpumpRoofPreset.COOL_TIMER1, new HeatpumpRoofService.SchedulerConfig(HeatpumpRoofPreset.DRY_TIMER, HeatpumpRoofPreset.COOL_AUTO, 60, null),
            HeatpumpRoofPreset.COOL_TIMER2, new HeatpumpRoofService.SchedulerConfig(HeatpumpRoofPreset.DRY_TIMER, HeatpumpRoofPreset.COOL_AUTO, 120, null),
            HeatpumpRoofPreset.COOL_TIMER3, new HeatpumpRoofService.SchedulerConfig(HeatpumpRoofPreset.DRY_TIMER, HeatpumpRoofPreset.COOL_AUTO, null, LocalTime.of(19,0))
    );

    private final Map<HeatpumpRoofPreset, List<HeatpumpRoofPreset>> conflictiongPresets = Map.of(
            HeatpumpRoofPreset.COOL_MIN, List.of(HeatpumpRoofPreset.HEAT_AUTO, HeatpumpRoofPreset.HEAT_MIN, HeatpumpRoofPreset.HEAT_TIMER1, HeatpumpRoofPreset.HEAT_TIMER2),
            HeatpumpRoofPreset.COOL_AUTO, List.of(HeatpumpRoofPreset.HEAT_AUTO, HeatpumpRoofPreset.HEAT_MIN, HeatpumpRoofPreset.HEAT_TIMER1, HeatpumpRoofPreset.HEAT_TIMER2),
            HeatpumpRoofPreset.HEAT_MIN, List.of(HeatpumpRoofPreset.COOL_AUTO, HeatpumpRoofPreset.COOL_MIN, HeatpumpRoofPreset.COOL_TIMER1, HeatpumpRoofPreset.COOL_TIMER2),
            HeatpumpRoofPreset.HEAT_AUTO, List.of(HeatpumpRoofPreset.COOL_AUTO, HeatpumpRoofPreset.COOL_MIN, HeatpumpRoofPreset.COOL_TIMER1, HeatpumpRoofPreset.COOL_TIMER2)
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
            try {
                refreshHeatpumpModel(true);
            } catch (Exception e) {
                handleException(e, "Could not call heatpump service (with-cache)");
            }
        }
    }

    private static void handleException(Exception e, String msg) {
        if(e instanceof RestClientException && e.getMessage().startsWith(ExternalServiceHttpAPI.MESSAGE_TOO_MANY_CALLS)){
            log.warn(msg + " - " + e.getMessage());
            return;
        }
        log.error(msg, e);
    }

    private boolean isRestartInTimerangeMinutes(int minutes){
        String serverRestartCron = env.getProperty("heatpump.roof.server.restartCron");
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
        request.setHeatpumpUsername(env.getProperty("heatpump.roof.driver.user"));
        request.setHeatpumpPassword(env.getProperty("heatpump.roof.driver.pass"));
        request.setReadFromCache(cachedData);

        HeatpumpResponse response = callDriver(request);
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
                CompletableFuture.runAsync(() -> pushService.sendErrorMessage("Fehler bei Ansteuerung von " + heatpumpName));
            }
            switchModelToUnknown();
            return;
        }

        HeatpumpRoofModel newModel = getHeatpumpModelWithUnknownPresets();
        response.getRoomnamesAndStates().forEach((r, s) -> {

            Place place = dictPlaceToRoomNameInDriver.entrySet().stream().filter(e ->
                    e.getValue().equalsIgnoreCase(r)).findFirst().orElseThrow().getKey();

            HeatpumpRoofPreset preset = lookupPresetFromModeAndSpeed(s);

            // upgrade to scheduled preset
            if(placeScheduler.get(place).isPresent() && placeScheduler.get(place).get().getBasePreset() == preset) {
                newModel.getHeatpumpMap().put(place, new HeatpumpRoof(
                        place, placeScheduler.get(place).get().getSchedulerPreset(), placeScheduler.get(place).get().getScheduledTimeInstant().toEpochMilli()));
            }else{
                newModel.getHeatpumpMap().put(place, new HeatpumpRoof(place, preset, null));
            }
        });

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    private HeatpumpRoofPreset lookupPresetFromModeAndSpeed(HeatpumpState s) {

        if(!s.getOnOffSwitch()){
            return HeatpumpRoofPreset.OFF;
        }else if(s.getMode() == HeatpumpMode.COOLING){
            if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                return HeatpumpRoofPreset.COOL_AUTO;
            }else{
                return HeatpumpRoofPreset.COOL_MIN;
            }
        }else if(s.getMode() == HeatpumpMode.HEATING){
            if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                return HeatpumpRoofPreset.HEAT_AUTO;
            }else{
                return HeatpumpRoofPreset.HEAT_MIN;
            }
        }else if(s.getMode() == HeatpumpMode.FAN){
            if(s.getFanSpeed() == HeatpumpFanSpeed.AUTO){
                return HeatpumpRoofPreset.FAN_AUTO;
            }else{
                return HeatpumpRoofPreset.FAN_MIN;
            }
        }else{
            return HeatpumpRoofPreset.UNKNOWN;
        }
    }

    private boolean responseHasError(HeatpumpResponse response) {
        return !response.isRemoteConnectionSuccessful() || !response.isDriverRunSuccessful() || StringUtils.isNotBlank(response.getErrorMessage());
    }

    public void timerPreset(List<Place> places, HeatpumpRoofPreset preset, Instant scheduledTime) {

        final List<Place> validTimerPlaces =
                places.stream().filter(p -> placeScheduler.get(p).isPresent() && placeScheduler.get(p).get().getScheduledTimeInstant() == scheduledTime).collect(Collectors.toList());

        preset(validTimerPlaces, preset);
    }

    public void preset(List<Place> places, HeatpumpRoofPreset preset) {

        if(places.isEmpty() || ModelObjectDAO.getInstance().readHeatpumpRoofModel() == null){
            return;
        }

        switchModelToBusy();

        cancelOldTimers(places);

        CompletableFuture.runAsync(() -> startPresetInternal(places, preset));
    }

    private synchronized void startPresetInternal(List<Place> places, HeatpumpRoofPreset preset) {

        Map<String, HeatpumpProgram> programs = new HashMap<>();
        places.forEach(p -> {
            final HeatpumpProgram program = presetToProgram(preset);
            final Map<Place, HeatpumpRoof> heatpumpMap = ModelObjectDAO.getInstance().readHeatpumpRoofModel().getHeatpumpMap();
            if (program != null && heatpumpMap.containsKey(p) && presetToProgram(heatpumpMap.get(p).getHeatpumpRoofPreset()) != program) {
                programs.put(dictPlaceToRoomNameInDriver.get(p), program);
            }
        });

        if(conflictiongPresets.containsKey(preset)){
            dictPlaceToRoomNameInDriver.keySet().stream().filter(p -> !places.contains(p)).forEach(px -> {
                if(conflictiongPresets.get(preset).contains(
                        ModelObjectDAO.getInstance().readHeatpumpRoofModel().getHeatpumpMap().get(px).getHeatpumpRoofPreset())){
                    programs.put(dictPlaceToRoomNameInDriver.get(px), HeatpumpProgram.OFF);
                }
            });
        }

        HeatpumpRequest request = new HeatpumpRequest();
        request.getReadWithRoomnames().addAll(dictPlaceToRoomNameInDriver.values());
        request.getWriteWithRoomnameAndProgram().putAll(programs);
        request.setHeatpumpUsername(env.getProperty("heatpump.roof.driver.user"));
        request.setHeatpumpPassword(env.getProperty("heatpump.roof.driver.pass"));
        request.setReadFromCache(false);

        final HeatpumpResponse response = callDriver(request);
        if(!responseHasError(response)){
            if(areExpectedModesSet(places, presetToProgram(preset), response)){
                scheduleNewTimers(places, preset);
            }else{
                CompletableFuture.runAsync(() -> pushService.sendErrorMessage(heatpumpName + " befindet sich nicht im erwarteten Modus!"));
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

    private void scheduleNewTimers(List<Place> places, HeatpumpRoofPreset preset) {

        if(schedulerConfigMap.containsKey(preset)){
            var config = schedulerConfigMap.get(preset);
            LocalDateTime scheduledTime;
            if(config.getTimeInMinutes() != null){
                scheduledTime = LocalDateTime.now().plusMinutes(config.getTimeInMinutes());
            }else{
                scheduledTime = LocalDateTime.of(LocalDate.now(), config.localTime);
                if(scheduledTime.isBefore(LocalDateTime.now()) && config.getLocalTime() != null){
                    if(preset == HeatpumpRoofPreset.HEAT_TIMER3){
                        scheduleNewTimers(places, HeatpumpRoofPreset.HEAT_TIMER2);
                        return;
                    }else if(preset == HeatpumpRoofPreset.COOL_TIMER3) {
                        scheduleNewTimers(places, HeatpumpRoofPreset.COOL_TIMER2);
                        return;
                    }
                    scheduledTime = LocalDateTime.now().plusMinutes(120);
                }
            }
            var scheduledTimeInstant = scheduledTime.atZone(ZoneId.systemDefault()).toInstant();
            final ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.schedule(() ->
                    timerPreset(places, config.getTarget(), scheduledTimeInstant), scheduledTimeInstant);
            places.forEach(p -> placeScheduler.put(p, Optional.of(new HeatpumpRoofService.SchedulerData(scheduledFuture, preset, config.getBase(), scheduledTimeInstant))));
        }
    }

    private void switchModelToBusy() {

        final HeatpumpRoofModel busyHeatpumpRoofModel = ModelObjectDAO.getInstance().readHeatpumpRoofModel();
        busyHeatpumpRoofModel.setBusy(true);
        busyHeatpumpRoofModel.setTimestamp(System.currentTimeMillis());
        ModelObjectDAO.getInstance().write(busyHeatpumpRoofModel);
        uploadService.uploadToClient(busyHeatpumpRoofModel);
    }

    private void switchModelToUnknown() {

        final HeatpumpRoofModel unknownHeatpumpRoofModel = getHeatpumpModelWithUnknownPresets();
        ModelObjectDAO.getInstance().write(unknownHeatpumpRoofModel);
        uploadService.uploadToClient(unknownHeatpumpRoofModel);
    }

    private HeatpumpRoofModel getHeatpumpModelWithUnknownPresets() {

        final var unknownHeatpumpModel = new HeatpumpRoofModel();
        unknownHeatpumpModel.setName(heatpumpName);
        unknownHeatpumpModel.setTimestamp(System.currentTimeMillis());
        dictPlaceToRoomNameInDriver.keySet().forEach(
                place -> unknownHeatpumpModel.getHeatpumpMap().put(place, new HeatpumpRoof(place, HeatpumpRoofPreset.UNKNOWN, null)));
        return unknownHeatpumpModel;
    }

    private HeatpumpProgram presetToProgram(HeatpumpRoofPreset preset){
        return switch (preset) {
            case COOL_AUTO, COOL_TIMER1, COOL_TIMER2, COOL_TIMER3 -> HeatpumpProgram.COOLING_AUTO;
            case COOL_MIN -> HeatpumpProgram.COOLING_MIN;
            case HEAT_AUTO -> HeatpumpProgram.HEATING_AUTO;
            case HEAT_MIN, HEAT_TIMER1, HEAT_TIMER2, HEAT_TIMER3 -> HeatpumpProgram.HEATING_MIN;
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
                    Objects.requireNonNull(env.getProperty("heatpump.roof.driver.url")), request);
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

        } catch (RestClientException e) {
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
        private HeatpumpRoofPreset schedulerPreset;
        private HeatpumpRoofPreset basePreset;
        private Instant scheduledTimeInstant;
    }

    @Data
    @AllArgsConstructor
    private static class SchedulerConfig{
        HeatpumpRoofPreset target;
        HeatpumpRoofPreset base;
        Integer timeInMinutes;
        LocalTime localTime;
    }

}
