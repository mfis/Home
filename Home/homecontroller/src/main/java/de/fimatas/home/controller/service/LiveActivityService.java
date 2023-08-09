package de.fimatas.home.controller.service;

import de.fimatas.home.controller.model.LiveActivityField;
import de.fimatas.home.controller.model.LiveActivityModel;
import de.fimatas.home.controller.model.LiveActivityType;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.HouseModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@CommonsLog
public class LiveActivityService {

    @Autowired
    private PushService pushService;

    private int low = 0;// FIXME

    private int high = 0;// FIXME

    private int ignore = 0;// FIXME

    @Scheduled(cron = "0 * 9-16 * * *") // FIXME
    public void testCounts() {
        LiveActivityModel liveActivity = new LiveActivityModel();
        liveActivity.setLiveActivityType(LiveActivityType.ELECTRICITY);
        processNewModelForSingleUser(ModelObjectDAO.getInstance().readHouseModel(), liveActivity);
    }

    @Scheduled(cron = "0 1 17 * * *") // FIXME
    public void testCountsReset() {
        log.warn("LIVE-ACTIVITY: IGNORE = " + ignore + ", LOW = " + low + ", HIGH = " + high);
        low = 0;
        high = 0;
        ignore = 0;
    }

    @Async
    public void newModel(Object object) {
        pushService.getActiveLiveActivities().forEach((token, liveActivity) -> processNewModelForSingleUser(object, liveActivity));
    }

    private void processNewModelForSingleUser(Object model, LiveActivityModel liveActivity) {

        List<MessagePriority> priorities = new ArrayList<>();

        if (model instanceof HouseModel) {
            // Values from HouseModel
            priorities.add(processValue(valueElectricGrid((HouseModel) model), liveActivity));
        }

        MessagePriority highestPriority = MessagePriority.getHighestPriority(priorities);
        if (highestPriority == MessagePriority.IGNORE) {
            ignore++;
        } else if (highestPriority == MessagePriority.LOW_PRIORITY) {
            // FIXME: SEND WITH LOW PRIORITY
            low++;
        } else if (highestPriority == MessagePriority.HIGH_PRIORITY) {
            // FIXME: SEND WITH HIGH PRIORITY
            liveActivity.shiftValuesToSentWithHighPriotity();
            high++;
        }
    }

    private MessagePriority processValue(FieldValue fieldValue, LiveActivityModel liveActivityModel) {
        if(fieldValue == null){
            return MessagePriority.IGNORE;
        }
        liveActivityModel.getActualValues().put(fieldValue.getField(), fieldValue.getValue());
        return calculateValueChangeEvaluation(fieldValue, liveActivityModel);
    }

    private FieldValue valueElectricGrid(HouseModel houseModel) {
        return houseModel.getGridElectricalPower() != null && houseModel.getGridElectricalPower().getActualConsumption() != null ?
                new FieldValue(houseModel.getGridElectricalPower().getActualConsumption().getValue(), LiveActivityField.ELECTRIC_GRID) : null;
    }

    private MessagePriority calculateValueChangeEvaluation(FieldValue fieldValue, LiveActivityModel model) {
        if (model.getLiveActivityType().fields().contains(fieldValue.getField())) {
            if (model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()) == null) {
                return MessagePriority.HIGH_PRIORITY;
            }
            if (model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()).compareTo(fieldValue.getValue()) == 0) {
                return MessagePriority.IGNORE;
            }
            BigDecimal diff = model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()).subtract(fieldValue.getValue()).abs();
            if (diff.compareTo(fieldValue.getField().getThresholdMin()) >= 0) {
                return MessagePriority.HIGH_PRIORITY;
            }
            return MessagePriority.LOW_PRIORITY;
        }
        return MessagePriority.IGNORE;
    }

    @Scheduled(cron = "20 0 0 * * *")
    public void endAll() {
        endAllActivities();
    }

    @PreDestroy
    public void preDestroy() {
        endAllActivities();
    }

    public void start(String token, String user, String device) {

        if (pushService.getActiveLiveActivities().containsKey(token)) {
            return;
        }

        pushService.getActiveLiveActivities().values().stream().filter(la -> la.getUsername().equals(user) && la.getDevice().equals(device))
                .forEach(la -> end(la.getToken()));

        var model = new LiveActivityModel();
        model.setToken(token);
        model.setUsername(user);
        model.setDevice(device);
        model.setLiveActivityType(LiveActivityType.ELECTRICITY);
        pushService.getActiveLiveActivities().put(token, model);
        sendToApns(token, true);
    }

    public void end(String token) {
        endActivitiy(token);
    }

    private synchronized void sendToApns(String token, boolean isFirstState) {

        // log.info("send live activity update: " + StringUtils.left(token, 10) + "...");
        LiveActivityModel model = pushService.getActiveLiveActivities().get(token);
        //boolean highPriority = isFirstState || LocalTime.now().getMinute() % 5 == 0;
        //noinspection UnnecessaryUnicodeEscape
        // FIXME: pushService.sendLiveActivityToApns(token, true, false, buildContentStateMap(true));
    }

    private Map<String, Object> buildContentStateMap(boolean withLiveValue) {

        SingleState primaryObject;
        SingleState secondaryObject;
        SingleState tertiaryObject;

        if (withLiveValue) {
            primaryObject = singleStateTime();
            secondaryObject = singleStateDate();
            tertiaryObject = singleStateEmpty();
        } else {
            primaryObject = singleStatePreview();
            secondaryObject = singleStatePreview();
            tertiaryObject = singleStatePreview();
        }

        Map<String, Object> contentState = new LinkedHashMap<>();
        contentState.put("contentId", UUID.randomUUID().toString());
        contentState.put("timestamp", LocalTime.now().format(DateTimeFormatter.ISO_TIME));
        contentState.put("dismissSeconds", "600"); // FIXME
        contentState.put("primary", buildSingleStateMap(primaryObject));
        contentState.put("secondary", buildSingleStateMap(secondaryObject));
        contentState.put("tertiary", buildSingleStateMap(tertiaryObject));

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

    private SingleState singleStatePreview() {
        var state = new SingleState();
        state.val = "--";
        state.symbolName = "square.dashed";
        state.symbolType = "sys";
        state.color = ".grey";
        return state;
    }

    private SingleState singleStateTime() {
        var state = new SingleState();
        state.val = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN).format(LocalDateTime.now());
        state.valShort = state.val;
        state.symbolName = "clock";
        state.symbolType = "sys";
        state.color = ".green";
        return state;
    }

    private SingleState singleStateDate() {
        var state = new SingleState();
        state.val = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN).format(LocalDateTime.now());
        state.valShort = state.val;
        state.symbolName = "calendar";
        state.symbolType = "sys";
        state.color = ".white";
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
            pushService.getActiveLiveActivities().keySet().forEach(this::endActivitiy);
        } catch (Exception e) {
            log.warn("Could not end LiveActivity via APNS");
        }
    }

    private void endActivitiy(String token) {
        try {
            pushService.sendLiveActivityToApns(token, true, true, buildContentStateMap(false));
        } catch (Exception e) {
            log.warn("Could not end LiveActivity via APNS: " + e.getMessage());
        }
        pushService.getActiveLiveActivities().remove(token);
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

    @Getter
    @AllArgsConstructor
    private static class FieldValue {
        private BigDecimal value;
        private LiveActivityField field;
    }

    private enum MessagePriority {
        IGNORE, LOW_PRIORITY, HIGH_PRIORITY;

        static MessagePriority getHighestPriority(List<MessagePriority> list){
            return list.stream()
                    .max(Comparator.comparing(Enum::ordinal))
                    .orElse(null);
        }
    }
}