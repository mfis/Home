package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.LiveActivityDAO;
import de.fimatas.home.controller.model.LiveActivityField;
import de.fimatas.home.controller.model.LiveActivityFieldValue;
import de.fimatas.home.controller.model.LiveActivityModel;
import de.fimatas.home.controller.model.LiveActivityType;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.AbstractSystemModel;
import de.fimatas.home.library.domain.model.ElectricVehicleModel;
import de.fimatas.home.library.model.PvAdditionalDataModel;
import de.fimatas.home.library.util.ViewFormatterUtils;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@CommonsLog
public class LiveActivityService {
    
    @Autowired
    private PushService pushService;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @Autowired
    private Environment env;

    private static final int MAX_UPDATES = 2000;

    private static final Duration STALE_DURATION = Duration.ofMinutes(20);

    protected static final Duration EQUAL_MODEL_STALE_PREVENTION_DURATION = Duration.ofMinutes(18);

    private static final Duration MAX_DISMISSAL_TIME = Duration.ofHours(8).minusMinutes(5);

    public static final String TEST_LOKEN_ONLY_LOGGING = "###_TESTTOKEN_###";

    @Async
    public <T extends AbstractSystemModel> void newModel(Class<T> clazz) {

        if (!Boolean.parseBoolean(env.getProperty("liveactivity.enabled"))) {
            return;
        }

        getActualModels().entrySet().stream()
                .filter(entry -> entry.getKey().equals(clazz))
                .findFirst()
                .ifPresent(entry -> processModel(entry.getKey(), entry.getValue()));
    }

    protected void processModel(Class<?> clazz, AbstractSystemModel model) {
        if(model == null) {
            return;
        }
        LiveActivityDAO.getInstance().getActiveLiveActivities().forEach((token, liveActivity) -> {
            synchronized (LiveActivityDAO.getInstance().getActiveLiveActivities().get(token)){
                if(liveActivity.getEndTimestamp().isAfter(Instant.now())){
                    List<MessagePriority> messagePriorityList = processNewModelForSingleUser(clazz, model, liveActivity);
                    sendToApns(liveActivity.getToken(), MessagePriority.getHighestPriority(messagePriorityList));
                }
            }
        });
    }

    @Scheduled(cron = "24 * * * * *")
    public void checkForDismissalDate() {
        List<String> tokensToRemove = new ArrayList<>();
        LiveActivityDAO.getInstance().getActiveLiveActivities().forEach((token, liveActivity) -> {
            if (liveActivity.getEndTimestamp().isBefore(Instant.now())) {
                tokensToRemove.add(token);
            }
        });
        tokensToRemove.forEach(this::endActivitiy);
    }

    @Scheduled(cron = "40 * * * * *")
    public void preventStaleCausedByEqualValues() {
        getActualModels().forEach(this::processModel);
    }

    private Map<Class<?>, AbstractSystemModel> getActualModels(){
        LinkedHashMap<Class<?>, AbstractSystemModel> map = new LinkedHashMap<>();
        map.put(PvAdditionalDataModel.class, ModelObjectDAO.getInstance().readPvAdditionalDataModel());
        map.put(ElectricVehicleModel.class, ModelObjectDAO.getInstance().readElectricVehicleModel());
        return map;
    }

    private List<MessagePriority> processNewModelForSingleUser(Class<?> clazz, Object model, LiveActivityModel liveActivity) {

        List<MessagePriority> priorities = new ArrayList<>();

        if(clazz.equals(PvAdditionalDataModel.class)) {
            var instance = (PvAdditionalDataModel) model;
            priorities.add(processValue(valuePvBattery(instance), liveActivity));
            priorities.add(processValue(valuePvProduction(instance), liveActivity));
            priorities.add(processValue(valueHouseConsumption(instance), liveActivity));
        }

        if(clazz.equals(ElectricVehicleModel.class)) {
            var instance = (ElectricVehicleModel) model;
            priorities.add(processValue(valueElectricVehicleCharge(instance), liveActivity));
        }

        return priorities;
    }

    private MessagePriority processValue(LiveActivityFieldValue liveActivityFieldValue, LiveActivityModel liveActivityModel) {
        if(liveActivityFieldValue.getValue() == null && !liveActivityFieldValue.isUnknown()){
            liveActivityModel.getActualValues().remove(liveActivityFieldValue.getField());
            return MessagePriority.IGNORE;
        }
        liveActivityModel.getActualValues().put(liveActivityFieldValue.getField(), liveActivityFieldValue);
        return calculateValueChangeEvaluation(liveActivityFieldValue, liveActivityModel);
    }

    private LiveActivityFieldValue valuePvProduction(PvAdditionalDataModel pvAdditionalDataModel) {
        var val = pvAdditionalDataModel == null ? null : pvAdditionalDataModel.getProductionWattage().getValue();
        return new LiveActivityFieldValue(val, LiveActivityField.PV_PRODUCTION, val == null);
    }

    private LiveActivityFieldValue valueHouseConsumption(PvAdditionalDataModel pvAdditionalDataModel) {
        var val = pvAdditionalDataModel == null ? null : pvAdditionalDataModel.getConsumptionWattage().getValue();
        return new LiveActivityFieldValue(val, LiveActivityField.HOUSE_CONSUMPTION, val == null);
    }

    private LiveActivityFieldValue valuePvBattery(PvAdditionalDataModel pvAdditionalDataModel) {
        var val = pvAdditionalDataModel == null ? null : new BigDecimal(ViewFormatterUtils.calculateViewPercentagePvBattery(pvAdditionalDataModel));
        return new LiveActivityFieldValue(val, LiveActivityField.PV_BATTERY, val == null);
    }

    private LiveActivityFieldValue valueElectricVehicleCharge(ElectricVehicleModel electricVehicleModel) {
        if(electricVehicleModel == null) {
            return new LiveActivityFieldValue(null, LiveActivityField.EV_CHARGE, true);
        }
        return electricVehicleModel.getEvMap().values().stream()
            .filter(evs -> evs.isActiveCharging() && evs.isConnectedToWallbox()).findFirst()
                .map(evs -> new LiveActivityFieldValue(new BigDecimal(ViewFormatterUtils.calculateViewPercentageEv(evs)), LiveActivityField.EV_CHARGE, false))
                .orElse(new LiveActivityFieldValue(null, LiveActivityField.EV_CHARGE, false));
    }

    private MessagePriority calculateValueChangeEvaluation(LiveActivityFieldValue liveActivityFieldValue, LiveActivityModel model) {
        var highestPriority = liveActivityFieldValue.getField().isAllowsHighPriority() ? MessagePriority.HIGH_PRIORITY : MessagePriority.LOW_PRIORITY;
        if (model.getLiveActivityType().fields().contains(liveActivityFieldValue.getField())) {
            // check for first value
            if (model.getLastValuesSentWithHighPriotity().get(liveActivityFieldValue.getField()) == null) {
                // log.info("LiveActivity calc: " + highestPriority.name() + " #1: " + fieldValue.field.name() + " = "  + fieldValue.getValue());
                return highestPriority;
            }
            // check for chenged unknown state
            if(model.getLastValuesSentWithHighPriotity().get(liveActivityFieldValue.getField()).isUnknown() != liveActivityFieldValue.isUnknown()) {
                return MessagePriority.HIGH_PRIORITY;
            }
            // check for equal value
            if (model.getLastValuesSentWithHighPriotity().get(liveActivityFieldValue.getField()).getValue().compareTo(liveActivityFieldValue.getValue()) == 0) {
                if(model.getLastValTimestampHighPriority().plus(EQUAL_MODEL_STALE_PREVENTION_DURATION).isBefore(uniqueTimestampService.getNonUnique())){
                    // log.info("LiveActivity STALE VERHINDERN");
                    return MessagePriority.HIGH_PRIORITY; // stale verhindern
                }
                return MessagePriority.IGNORE;
            }
            // check for different signs
            BigDecimal product = model.getLastValuesSentWithHighPriotity().get(liveActivityFieldValue.getField()).getValue().multiply(liveActivityFieldValue.getValue());
            if(product.compareTo(BigDecimal.ZERO) < 0){
                // log.info("LiveActivity calc: " + highestPriority.name() + " #2: " + fieldValue.field.name() + " = "  + fieldValue.getValue());
                return highestPriority;
            }
            // check for threshold
            BigDecimal diff = model.getLastValuesSentWithHighPriotity().get(liveActivityFieldValue.getField()).getValue().subtract(liveActivityFieldValue.getValue()).abs();
            if (diff.compareTo(liveActivityFieldValue.getField().getThresholdMin()) >= 0) {
                // log.info("LiveActivity calc: " + highestPriority.name() + " #3: " + fieldValue.field.name() + " = "  + fieldValue.getValue());
                return highestPriority;
            }

            // check for equal value with low priority
            if (model.getLastValuesSentWithLowPriotity().containsKey(liveActivityFieldValue.getField()) &&
                    model.getLastValuesSentWithLowPriotity().get(liveActivityFieldValue.getField()).getValue().compareTo(liveActivityFieldValue.getValue()) == 0) {
                return MessagePriority.IGNORE;
            }

            // log.info("LiveActivity calc: " + MessagePriority.LOW_PRIORITY.name() + " #4: " + fieldValue.field.name() + " = "  + fieldValue.getValue() + " (" + model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()) + ")");
            return MessagePriority.LOW_PRIORITY;
        }
        return MessagePriority.IGNORE;
    }

    @Scheduled(cron = "22 0 0 * * *")
    public void endAll() {
        endAllActivities();
    }

    @PreDestroy
    public void preDestroy() {
        log.info("LiveActivity preDestroy()");
        endAllActivities();
    }

    public void start(String token, String user, String device) {

        if (!Boolean.parseBoolean(env.getProperty("liveactivity.enabled"))) {
            return;
        }

        if (LiveActivityDAO.getInstance().getActiveLiveActivities().containsKey(token)) {
            return;
        }

        log.info("LiveActivity start: " + user);

        LiveActivityDAO.getInstance().getActiveLiveActivities().values().stream().filter(la -> la.getUsername().equals(user) && la.getDevice().equals(device))
                .forEach(la -> endActivitiy(la.getToken()));

        var liveActivity = new LiveActivityModel();
        liveActivity.setToken(token);
        liveActivity.setUsername(user);
        liveActivity.setDevice(device);
        liveActivity.setStartTimestamp(Instant.now());
        liveActivity.setEndTimestamp(Instant.now().plus(MAX_DISMISSAL_TIME));
        liveActivity.setLiveActivityType(LiveActivityType.ELECTRICITY);
        LiveActivityDAO.getInstance().getActiveLiveActivities().put(token, liveActivity);

        getActualModels().forEach((key, value) -> processNewModelForSingleUser(key, value, liveActivity));

        sendToApns(liveActivity.getToken(), MessagePriority.HIGH_PRIORITY);
    }

    private void sendToApns(String token, MessagePriority messagePriority) {

        if (messagePriority == MessagePriority.IGNORE) {
            return;
        }

        LiveActivityModel liveActivityModel = LiveActivityDAO.getInstance().getActiveLiveActivities().get(token);
        liveActivityModel.setUpdateCounter(liveActivityModel.getUpdateCounter() +1);
        if(liveActivityModel.getUpdateCounter() > MAX_UPDATES){
            return;
        }

        boolean highPriority = messagePriority == MessagePriority.HIGH_PRIORITY;

        if(token.equals(TEST_LOKEN_ONLY_LOGGING)){
            log.info("TEST_LIVE_ACTIVITY: prio=" + highPriority + ", map=" + buildContentStateMap(token, true));
        }else{
            //noinspection UnnecessaryUnicodeEscape
            log.debug("LiveActivity PUSH to: " + liveActivityModel.getUsername() + ", prio=" + messagePriority);
            pushService.sendLiveActivityToApns(token, highPriority, false, STALE_DURATION, liveActivityModel.getEndTimestamp(), buildContentStateMap(token, true));
        }

        if (messagePriority == MessagePriority.HIGH_PRIORITY) {
            liveActivityModel.shiftValuesToSentWithHighPriotity(uniqueTimestampService.getNonUnique());
        } else {
            liveActivityModel.shiftValuesLowPriotity(uniqueTimestampService.getNonUnique());
        }
    }

    private Map<String, Object> buildContentStateMap(String token, boolean withLiveValue) {

        SingleState primaryObject;
        SingleState secondaryObject;
        SingleState tertiaryObject;
        SingleState quaternaryObject;

        if (withLiveValue) {
            primaryObject = singleStateValues(token, LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getLiveActivityType().getPrimary());
            secondaryObject = singleStateValues(token, LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getLiveActivityType().getSecondary());
            tertiaryObject = singleStateValues(token, LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getLiveActivityType().getTertiary());
            quaternaryObject = singleStateValues(token, LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getLiveActivityType().getQuaternary());
        } else {
            primaryObject = singleStatePreview();
            secondaryObject = singleStatePreview();
            tertiaryObject = singleStatePreview();
            quaternaryObject = singleStatePreview();
        }

        Map<String, Object> contentState = new LinkedHashMap<>();
        contentState.put("contentId", UUID.randomUUID().toString());
        contentState.put("timestamp", LocalTime.now().format(DateTimeFormatter.ISO_TIME));
        contentState.put("primary", buildSingleStateMap(primaryObject));
        contentState.put("secondary", buildSingleStateMap(secondaryObject));
        contentState.put("tertiary", buildSingleStateMap(tertiaryObject));
        contentState.put("quaternary", buildSingleStateMap(quaternaryObject));

        return contentState;
    }

    private SingleState singleStateEmpty() {
        var state = new SingleState();
        state.val = "";
        state.symbolName = "";
        state.symbolType = "";
        state.color = "";
        return state;
    }

    private SingleState singleStateValues(String token, LiveActivityField field) {

        final var val = LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getActualValues().get(field);
        if(val == null || val.getValue()==null && !val.isUnknown()){
            return singleStateEmpty();
        }

        var state = new SingleState();
        state.label = field.getLabel();
        state.symbolType = field.getSymbolType();
        state.symbolName = field.getSymbolName().apply(val.getValue());
        state.color = field.color(val.getValue());
        if(val.isUnknown()){
            state.val = "?";
            state.valShort = "?";
        }else {
            state.val = field.formatValue(val.getValue());
            state.valShort = field.formatShort(val.getValue());
        }
        return state;
    }

    private SingleState singleStatePreview() {
        var state = new SingleState();
        state.label = "";
        state.val = "--";
        state.valShort = "-";
        state.symbolName = "square.dashed";
        state.symbolType = "sys";
        state.color = ".grey";
        return state;
    }

    private Map<String, Object> buildSingleStateMap(SingleState state) {
        Map<String, Object> singleStateMap = new LinkedHashMap<>();
        singleStateMap.put("symbolName", StringUtils.trimToEmpty(state.symbolName));
        singleStateMap.put("symbolType", StringUtils.trimToEmpty(state.symbolType));
        singleStateMap.put("label", StringUtils.trimToEmpty(state.label));
        singleStateMap.put("val", StringUtils.trimToEmpty(state.val));
        singleStateMap.put("valShort", StringUtils.trimToEmpty(state.valShort));
        singleStateMap.put("color", StringUtils.trimToEmpty(state.color));
        return singleStateMap;
    }

    private void endAllActivities() {
        try {
            new ArrayList<>(LiveActivityDAO.getInstance().getActiveLiveActivities().keySet()).forEach(this::endActivitiy);
        } catch (Exception e) {
            log.warn("LiveActivity endAllActivities(): Could not end LiveActivity via APNS: " + e.getMessage());
        }
    }

    public void endActivitiyFromClient(String token) {
        log.info("LiveActivity endActivitiyFromClient()");
        endActivitiy(token);
    }

    private void endActivitiy(String token) {
        synchronized (this) {
            try {
                log.info("LiveActivity END");
                pushService.sendLiveActivityToApns(token, true, true, Duration.ZERO, Instant.now(), buildContentStateMap(token, false));
            } catch (Throwable t) {
                log.warn("LiveActivity endActivitiy(): Could not end LiveActivity via APNS: " + t.getMessage());
            }
            LiveActivityDAO.getInstance().getActiveLiveActivities().remove(token);
        }
    }

    @Data
    private static class SingleState {
        private String symbolName;
        private String symbolType;
        private String label;
        private String val;
        private String valShort;
        private String color;
    }

    private enum MessagePriority {
        IGNORE, LOW_PRIORITY, HIGH_PRIORITY;

        static MessagePriority getHighestPriority(List<MessagePriority> list){
            return list.stream()
                    .max(Comparator.comparing(Enum::ordinal))
                    .orElse(IGNORE);
        }
    }
}