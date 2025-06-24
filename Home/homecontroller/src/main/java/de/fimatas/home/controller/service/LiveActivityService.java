package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.LiveActivityDAO;
import de.fimatas.home.controller.model.LiveActivityField;
import de.fimatas.home.controller.model.LiveActivityModel;
import de.fimatas.home.controller.model.LiveActivityType;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.ElectricVehicleModel;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.model.PvAdditionalDataModel;
import de.fimatas.home.library.util.ViewFormatterUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@CommonsLog
public class LiveActivityService {

    @Autowired
    private PushService pushService;

    private static final Integer DISMISS_SECONDS = 600;

    private static final int MAX_UPDATES = 2500;

    public static final String TEST_LOKEN_ONLY_LOGGING = "###_TESTTOKEN_###";

    @Async
    public void newModel(Object object) {
        LiveActivityDAO.getInstance().getActiveLiveActivities().forEach((token, liveActivity) -> {
            synchronized (LiveActivityDAO.getInstance().getActiveLiveActivities().get(token)){
                List<MessagePriority> messagePriorityList = processNewModelForSingleUser(object, liveActivity);
                sendToApns(liveActivity.getToken(), MessagePriority.getHighestPriority(messagePriorityList));
            }
        });
    }

    private List<MessagePriority> processNewModelForSingleUser(Object model, LiveActivityModel liveActivity) {

        List<MessagePriority> priorities = new ArrayList<>();

        if (model instanceof HouseModel) {
            priorities.add(processValue(valuePvProduction((HouseModel) model), liveActivity));
            priorities.add(processValue(valueHouseConsumption((HouseModel) model), liveActivity));
        }

        if(model instanceof PvAdditionalDataModel) {
            priorities.add(processValue(valuePvBattery((PvAdditionalDataModel) model), liveActivity));
        }

        if(model instanceof ElectricVehicleModel) {
            priorities.add(processValue(valueElectricVehicle((ElectricVehicleModel) model), liveActivity));
        }

        return priorities;
    }

    private MessagePriority processValue(FieldValue fieldValue, LiveActivityModel liveActivityModel) {
        if(fieldValue.getValue() == null){
            liveActivityModel.getActualValues().remove(fieldValue.getField());
            return MessagePriority.IGNORE;
        }
        liveActivityModel.getActualValues().put(fieldValue.getField(), fieldValue.getValue());
        return calculateValueChangeEvaluation(fieldValue, liveActivityModel);
    }

    private FieldValue valuePvProduction(HouseModel houseModel) {
        return new FieldValue(houseModel.getProducedElectricalPower() != null
                && houseModel.getProducedElectricalPower().getActualConsumption() != null ?
                houseModel.getProducedElectricalPower().getActualConsumption().getValue() : null, LiveActivityField.PV_PRODUCTION);
    }

    private FieldValue valueHouseConsumption(HouseModel houseModel) {
        return new FieldValue(houseModel.getConsumedElectricalPower() != null
                && houseModel.getConsumedElectricalPower().getActualConsumption() != null ?
                houseModel.getConsumedElectricalPower().getActualConsumption().getValue() : null, LiveActivityField.HOUSE_CONSUMPTION);
    }

    private FieldValue valuePvBattery(PvAdditionalDataModel pvAdditionalDataModel) {
        return new FieldValue(new BigDecimal(ViewFormatterUtils.calculateViewPercentagePvBattery(pvAdditionalDataModel)), LiveActivityField.PV_BATTERY);
    }

    private FieldValue valueElectricVehicle(ElectricVehicleModel electricVehicleModel) {
        return electricVehicleModel.getEvMap().values().stream()
            .filter(evs -> evs.isActiveCharging() && evs.isConnectedToWallbox()).findFirst()
                .map(evs -> new FieldValue(new BigDecimal(ViewFormatterUtils.calculateViewPercentageEv(evs)), LiveActivityField.EV_CHARGE))
                .orElse(new FieldValue(null, LiveActivityField.EV_CHARGE));
    }

    private MessagePriority calculateValueChangeEvaluation(FieldValue fieldValue, LiveActivityModel model) {
        var highestPriority = fieldValue.getField().isAllowsHighPriority() ? MessagePriority.HIGH_PRIORITY : MessagePriority.LOW_PRIORITY;
        if (model.getLiveActivityType().fields().contains(fieldValue.getField())) {
            // check for first value
            if (model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()) == null) {
                log.debug(highestPriority.name() + " #1: " + fieldValue.field.name() + " = "  + fieldValue.getValue());
                return highestPriority;
            }
            // check for equal value
            if (model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()).compareTo(fieldValue.getValue()) == 0) {
                return MessagePriority.IGNORE;
            }
            // check for different signs
            BigDecimal product = model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()).multiply(fieldValue.getValue());
            if(product.compareTo(BigDecimal.ZERO) < 0){
                log.debug(highestPriority.name() + " #2: " + fieldValue.field.name() + " = "  + fieldValue.getValue());
                return highestPriority;
            }
            // check for threshold
            BigDecimal diff = model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()).subtract(fieldValue.getValue()).abs();
            if (diff.compareTo(fieldValue.getField().getThresholdMin()) >= 0) {
                log.debug(highestPriority.name() + " #3: " + fieldValue.field.name() + " = "  + fieldValue.getValue());
                return highestPriority;
            }

            // check for equal value with low priority
            if (model.getLastValuesSentWithLowPriotity().containsKey(fieldValue.getField()) &&
                    model.getLastValuesSentWithLowPriotity().get(fieldValue.getField()).compareTo(fieldValue.getValue()) == 0) {
                return MessagePriority.IGNORE;
            }

            log.debug(MessagePriority.LOW_PRIORITY.name() + " #4: " + fieldValue.field.name() + " = "  + fieldValue.getValue() + " (" + model.getLastValuesSentWithHighPriotity().get(fieldValue.getField()) + ")");
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
        endAllActivities();
    }

    public void start(String token, String user, String device) {

        if (LiveActivityDAO.getInstance().getActiveLiveActivities().containsKey(token)) {
            return;
        }

        LiveActivityDAO.getInstance().getActiveLiveActivities().values().stream().filter(la -> la.getUsername().equals(user) && la.getDevice().equals(device))
                .forEach(la -> end(la.getToken()));

        var liveActivity = new LiveActivityModel();
        liveActivity.setToken(token);
        liveActivity.setUsername(user);
        liveActivity.setDevice(device);
        liveActivity.setLiveActivityType(LiveActivityType.ELECTRICITY);
        LiveActivityDAO.getInstance().getActiveLiveActivities().put(token, liveActivity);

        processNewModelForSingleUser(ModelObjectDAO.getInstance().readHouseModel(), liveActivity);
        processNewModelForSingleUser(ModelObjectDAO.getInstance().readPvAdditionalDataModel(), liveActivity);
        processNewModelForSingleUser(ModelObjectDAO.getInstance().readElectricVehicleModel(), liveActivity);
        sendToApns(liveActivity.getToken(), MessagePriority.HIGH_PRIORITY);
    }

    public void end(String token) {
        endActivitiy(token);
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
            log.info("LiveActivity Push to: " + liveActivityModel.getUsername() + ", prio=" + highPriority);
            pushService.sendLiveActivityToApns(token, highPriority, false, buildContentStateMap(token, true));
        }

        if (messagePriority == MessagePriority.HIGH_PRIORITY) {
            liveActivityModel.shiftValuesToSentWithHighPriotity();
        } else {
            liveActivityModel.shiftValuesLowPriotity();
        }
    }

    private Map<String, Object> buildContentStateMap(String token, boolean withLiveValue) {

        SingleState primaryObject;
        SingleState secondaryObject;
        SingleState tertiaryObject;

        if (withLiveValue) {
            primaryObject = singleStateValues(token, LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getLiveActivityType().getPrimary());
            secondaryObject = singleStateValues(token, LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getLiveActivityType().getSecondary());
            tertiaryObject = singleStateValues(token, LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getLiveActivityType().getTertiary());
        } else {
            primaryObject = singleStatePreview();
            secondaryObject = singleStatePreview();
            tertiaryObject = singleStatePreview();
        }

        Map<String, Object> contentState = new LinkedHashMap<>();
        contentState.put("contentId", UUID.randomUUID().toString());
        contentState.put("timestamp", LocalTime.now().format(DateTimeFormatter.ISO_TIME));
        contentState.put("dismissSeconds", DISMISS_SECONDS.toString());
        contentState.put("primary", buildSingleStateMap(primaryObject));
        contentState.put("secondary", buildSingleStateMap(secondaryObject));
        contentState.put("tertiary", buildSingleStateMap(tertiaryObject));
        contentState.put("quaternary", buildSingleStateMap(singleStatePreview()));

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

        final BigDecimal value = LiveActivityDAO.getInstance().getActiveLiveActivities().get(token).getActualValues().get(field);
        if(value==null){
            return singleStateEmpty();
        }

        var state = new SingleState();
        state.label = field.getLabel();
        state.val = field.formatValue(value);
        state.valShort = field.formatShort(value);
        state.symbolName = field.getSymbolName().apply(value);
        state.symbolType = field.getSymbolType();
        state.color = field.color(value);
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
            LiveActivityDAO.getInstance().getActiveLiveActivities().keySet().forEach(this::endActivitiy);
        } catch (Exception e) {
            log.warn("Could not end LiveActivity via APNS");
        }
    }

    private void endActivitiy(String token) {

        if(LiveActivityDAO.getInstance().getActiveLiveActivities().containsKey(token)){
            synchronized (LiveActivityDAO.getInstance().getActiveLiveActivities().get(token)) {
                try {
                    pushService.sendLiveActivityToApns(token, true, true, buildContentStateMap(token, false));
                } catch (Exception e) {
                    log.warn("Could not end LiveActivity via APNS: " + e.getMessage());
                }
                LiveActivityDAO.getInstance().getActiveLiveActivities().remove(token);
            }
        }else{
            // occurs after restart while liveactivity is running on a device
            synchronized (this) {
                try {
                    pushService.sendLiveActivityToApns(token, true, true, buildContentStateMap(token, false));
                } catch (Exception e) {
                    log.warn("Could not end LiveActivity via APNS: " + e.getMessage());
                }
            }
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