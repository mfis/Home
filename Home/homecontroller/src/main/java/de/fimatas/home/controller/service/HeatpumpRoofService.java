package de.fimatas.home.controller.service;

import de.fimatas.home.controller.api.TasmotaAPI;
import de.fimatas.home.controller.command.PersistentCacheCommand;
import de.fimatas.home.controller.dao.PersistentCacheDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.HeatpumpRoof;
import de.fimatas.home.library.domain.model.HeatpumpRoofModel;
import de.fimatas.home.library.domain.model.HeatpumpRoofPreset;
import de.fimatas.home.library.domain.model.Place;
import de.fimatas.home.library.model.PersistentCacheEntry;
import de.fimatas.home.library.model.PersistentCacheKey;
import de.fimatas.home.library.util.HomeAppConstants;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.*;
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
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private TasmotaAPI tasmotaAPI;

    @Autowired
    private Environment env;

    @Autowired
    private PersistentCacheDAO  persistentCacheDAO;

    @Value("${heatpump.roof.name:UnbekannteWaermepumpe}")
    private String heatpumpName;

    private Map<Place, String> dictPlaceToRoomNameInDriver;

    private Map<Place, Optional<SchedulerData>> placeScheduler;

    private boolean initDone = false;

    private final Map<HeatpumpRoofPreset, SchedulerConfig> schedulerConfigMap = Map.of(

            HeatpumpRoofPreset.DRY_TIMER, new SchedulerConfig(HeatpumpRoofPreset.OFF, HeatpumpRoofPreset.DRY_TIMER, HomeAppConstants.HEATPUMP_DRY_TIMER_DURATION_MINUTES, null),

            HeatpumpRoofPreset.HEAT_TIMER1, new SchedulerConfig(HeatpumpRoofPreset.OFF, HeatpumpRoofPreset.HEAT_TIMER1, 120, null),
            HeatpumpRoofPreset.HEAT_TIMER2, new SchedulerConfig(HeatpumpRoofPreset.OFF, HeatpumpRoofPreset.HEAT_TIMER2, null, LocalTime.of(13,0)),

            HeatpumpRoofPreset.COOL1_TIMER1, new SchedulerConfig(HeatpumpRoofPreset.DRY_TIMER, HeatpumpRoofPreset.COOL1_TIMER1, null, LocalTime.of(19,30)),
            HeatpumpRoofPreset.COOL1_TIMER2, new SchedulerConfig(HeatpumpRoofPreset.DRY_TIMER, HeatpumpRoofPreset.COOL1_TIMER2, null, LocalTime.of(22,0)),

            HeatpumpRoofPreset.COOL2_TIMER1, new SchedulerConfig(HeatpumpRoofPreset.DRY_TIMER, HeatpumpRoofPreset.COOL2_TIMER1, null, LocalTime.of(19,30)),
            HeatpumpRoofPreset.COOL2_TIMER2, new SchedulerConfig(HeatpumpRoofPreset.DRY_TIMER, HeatpumpRoofPreset.COOL2_TIMER2, null, LocalTime.of(22,0))
    );

    private final Map<HeatpumpRoofPreset, List<HeatpumpRoofPreset>> conflictiongPresetsCool1 = Map.of(

            HeatpumpRoofPreset.COOL1_AUTO, List.of(HeatpumpRoofPreset.HEAT_AUTO, HeatpumpRoofPreset.HEAT_TIMER1, HeatpumpRoofPreset.HEAT_TIMER2),
            HeatpumpRoofPreset.COOL1_TIMER1, List.of(HeatpumpRoofPreset.HEAT_AUTO, HeatpumpRoofPreset.HEAT_TIMER1, HeatpumpRoofPreset.HEAT_TIMER2),
            HeatpumpRoofPreset.COOL1_TIMER2, List.of(HeatpumpRoofPreset.HEAT_AUTO, HeatpumpRoofPreset.HEAT_TIMER1, HeatpumpRoofPreset.HEAT_TIMER2)
    );

    private final Map<HeatpumpRoofPreset, List<HeatpumpRoofPreset>> conflictiongPresetsCool2 = Map.of(

            HeatpumpRoofPreset.COOL2_AUTO, List.of(HeatpumpRoofPreset.HEAT_AUTO, HeatpumpRoofPreset.HEAT_TIMER1, HeatpumpRoofPreset.HEAT_TIMER2),
            HeatpumpRoofPreset.COOL2_TIMER1, List.of(HeatpumpRoofPreset.HEAT_AUTO, HeatpumpRoofPreset.HEAT_TIMER1, HeatpumpRoofPreset.HEAT_TIMER2),
            HeatpumpRoofPreset.COOL2_TIMER2, List.of(HeatpumpRoofPreset.HEAT_AUTO, HeatpumpRoofPreset.HEAT_TIMER1, HeatpumpRoofPreset.HEAT_TIMER2)
    );

    private final Map<HeatpumpRoofPreset, List<HeatpumpRoofPreset>> conflictiongPresetsHeat = Map.of(

            HeatpumpRoofPreset.HEAT_AUTO, List.of(HeatpumpRoofPreset.COOL1_AUTO, HeatpumpRoofPreset.COOL1_TIMER1, HeatpumpRoofPreset.COOL1_TIMER2, HeatpumpRoofPreset.COOL1_TIMER2),
            HeatpumpRoofPreset.HEAT_TIMER1, List.of(HeatpumpRoofPreset.COOL1_AUTO, HeatpumpRoofPreset.COOL1_TIMER1, HeatpumpRoofPreset.COOL1_TIMER2, HeatpumpRoofPreset.COOL1_TIMER2),
            HeatpumpRoofPreset.HEAT_TIMER2, List.of(HeatpumpRoofPreset.COOL1_AUTO, HeatpumpRoofPreset.COOL1_TIMER1, HeatpumpRoofPreset.COOL1_TIMER2, HeatpumpRoofPreset.COOL1_TIMER2)
    );

    private final Map<HeatpumpRoofPreset, List<HeatpumpRoofPreset>> conflictiongPresets = new HashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void startup() {

        conflictiongPresets.putAll(conflictiongPresetsCool1);
        conflictiongPresets.putAll(conflictiongPresetsCool2);
        conflictiongPresets.putAll(conflictiongPresetsHeat);

        dictPlaceToRoomNameInDriver = Map.of( //
                Place.BEDROOM, Place.BEDROOM.getPlaceName(), //
                Place.KIDSROOM_1, Objects.requireNonNull(env.getProperty("place.KIDSROOM_1.subtitle")), //
                Place.KIDSROOM_2, Objects.requireNonNull(env.getProperty("place.KIDSROOM_2.subtitle")) //
        );

        placeScheduler = new EnumMap<>(Place.class);
        dictPlaceToRoomNameInDriver.keySet().forEach(place -> placeScheduler.put(place, Optional.empty()));

        PersistentCacheEntry<HeatpumpRoofModel> cachedModel = persistentCacheDAO.read(PersistentCacheKey.HEATPUMP_ROOF, HeatpumpRoofModel.class);

        if(cachedModel == null) {
            switchModelToUnknown();
        } else {
            if (cachedModel.getTimestamp().isBefore(LocalDateTime.now().minus(Duration.ofMinutes(20)))) {
                switchModelToUnknown(); // ignore older models
            } else{
                ModelObjectDAO.getInstance().write(cachedModel.getValue());
            }
        }

        initDone = true;
        scheduledRefresh();
    }

    @PreDestroy
    public void preDestroy() {
        HeatpumpRoofModel model = ModelObjectDAO.getInstance().readHeatpumpRoofModel();
        if(model != null) {
            persistentCacheDAO.write(new PersistentCacheCommand(PersistentCacheKey.HEATPUMP_ROOF, model));
        }
    }

    @Scheduled(cron = "50 4/10 * * * *")
    public void scheduledRefresh() {
        if(initDone) {
            ModelObjectDAO.getInstance().models().values().stream().filter(model -> model instanceof HeatpumpRoofModel).findFirst().ifPresent(model -> {
                ModelObjectDAO.getInstance().write((HeatpumpRoofModel) model); // update timestamp
                uploadService.uploadToClient(ModelObjectDAO.getInstance().readHeatpumpRoofModel());
            });
        }
    }

    private void timerPreset(List<Place> places, HeatpumpRoofPreset preset, Instant scheduledTime) {

        final List<Place> validTimerPlaces =
                places.stream().filter(p -> placeScheduler.get(p).isPresent() && placeScheduler.get(p).get().getScheduledTimeInstant() == scheduledTime).collect(Collectors.toList());

        preset(validTimerPlaces, preset);
    }

    public void preset(List<Place> places, HeatpumpRoofPreset preset) {

        log.debug("preset " + places + " " + preset);
        if(places.isEmpty() || ModelObjectDAO.getInstance().readHeatpumpRoofModel() == null){
            return;
        }

        switchModelToBusy();

        cancelOldTimers(places);

        CompletableFuture.runAsync(() -> {
            try {
                startPresetInternal(places, preset);
            } catch (Exception e) {
                log.error("error while starting preset", e);
            }
        });
    }

    private synchronized void startPresetInternal(List<Place> places, HeatpumpRoofPreset preset) {

        Map<String, HeatpumpRoofPreset> programs = new HashMap<>();
        places.forEach(p -> {
            final Map<Place, HeatpumpRoof> heatpumpMap = ModelObjectDAO.getInstance().readHeatpumpRoofModel().getHeatpumpMap();
            if (preset != null && heatpumpMap.containsKey(p) && heatpumpMap.get(p).getHeatpumpRoofPreset() != preset) {
                programs.put(dictPlaceToRoomNameInDriver.get(p), preset);
            }
        });

        if(conflictiongPresets.containsKey(preset)){
            dictPlaceToRoomNameInDriver.keySet().stream().filter(p -> !places.contains(p)).forEach(px -> {
                if(conflictiongPresets.get(preset).contains(
                        ModelObjectDAO.getInstance().readHeatpumpRoofModel().getHeatpumpMap().get(px).getHeatpumpRoofPreset())){
                    programs.put(dictPlaceToRoomNameInDriver.get(px), HeatpumpRoofPreset.OFF);
                }
            });
        }

        var responses = callAPI(programs);

        HeatpumpRoofModel newModel = SerializationUtils.clone(ModelObjectDAO.getInstance().readHeatpumpRoofModel());
        newModel.setBusy(false);
        var placesSuccessful = new ArrayList<Place>();
        var roomnamesForErrorPush = new ArrayList<String>();

        places.forEach((place) -> {
            var roomname = dictPlaceToRoomNameInDriver.get(place);
            if(!responses.containsKey(roomname)) {
                placesSuccessful.add(place); // not changed mode is still successful
            } else if(responses.get(roomname) == true){
                placesSuccessful.add(place);
            } else {
                roomnamesForErrorPush.add(roomname);
                newModel.getHeatpumpMap().put(place, new HeatpumpRoof(place, HeatpumpRoofPreset.UNKNOWN, null));
            }
        });

        scheduleNewTimers(placesSuccessful, preset);
        placesSuccessful.forEach((place) -> {
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

        if(!roomnamesForErrorPush.isEmpty()){
            CompletableFuture.runAsync(() -> pushService.sendErrorMessage("Fehler bei Ansteuerung von Wärmepumpe: " + roomnamesForErrorPush));
        }
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

        if(places.isEmpty()){
            return;
        }

        if(schedulerConfigMap.containsKey(preset)){
            var config = schedulerConfigMap.get(preset);
            LocalDateTime scheduledTime;
            if(config.getTimeInMinutes() != null){
                scheduledTime = LocalDateTime.now().plusMinutes(config.getTimeInMinutes());
            }else{
                scheduledTime = LocalDateTime.of(LocalDate.now(), config.localTime);
                if(scheduledTime.isBefore(LocalDateTime.now()) && config.getLocalTime() != null){
                    scheduledTime = LocalDateTime.now().plusMinutes(120); // fallback
                }
            }
            var scheduledTimeInstant = scheduledTime.atZone(ZoneId.systemDefault()).toInstant();
            final ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.schedule(() ->
                    timerPreset(places, config.getTarget(), scheduledTimeInstant), scheduledTimeInstant);
            places.forEach(p -> placeScheduler.put(p, Optional.of(new SchedulerData(scheduledFuture, preset, config.getBase(), scheduledTimeInstant))));
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

        final HeatpumpRoofModel unknownHeatpumpRoofModel = getHeatpumpRoofModelWithUnknownPresets();
        ModelObjectDAO.getInstance().write(unknownHeatpumpRoofModel);
        uploadService.uploadToClient(unknownHeatpumpRoofModel);
    }

    private HeatpumpRoofModel getHeatpumpRoofModelWithUnknownPresets() {

        final var unknownHeatpumpRoofModel = new HeatpumpRoofModel();
        unknownHeatpumpRoofModel.setName(heatpumpName);
        unknownHeatpumpRoofModel.setTimestamp(System.currentTimeMillis());
        dictPlaceToRoomNameInDriver.keySet().forEach(
                place -> unknownHeatpumpRoofModel.getHeatpumpMap().put(place, new HeatpumpRoof(place, HeatpumpRoofPreset.UNKNOWN, null)));
        return unknownHeatpumpRoofModel;
    }

    private synchronized Map<String, Boolean> callAPI(Map<String, HeatpumpRoofPreset> programs){
        return tasmotaAPI.call(programs);
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
