package de.fimatas.home.controller.service;

import de.fimatas.home.controller.model.LiveActivityModel;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@CommonsLog
public class LiveActivityService {

    @Autowired
    private PushService pushService;

    @Scheduled(cron = "0 * * * * *")
    public void run(){
        pushService.getActiveLiveActivities().keySet().forEach(this::sendToApns);
    }

    @Scheduled(cron = "20 0 0 * * *")
    public void endAll(){
        endAllActivities();
    }

    @PreDestroy
    public void preDestroy(){
       endAllActivities();
    }

    public void start(String token, String user, String device){

        if(pushService.getActiveLiveActivities().containsKey(token)){
            return;
        }

        pushService.getActiveLiveActivities().values().stream().filter(la -> la.getUsername().equals(user) && la.getDevice().equals(device))
                .forEach(la -> end(la.getToken()));

        var model = new LiveActivityModel();
        model.setToken(token);
        model.setUsername(user);
        model.setDevice(device);
        pushService.getActiveLiveActivities().put(token, model);
        sendToApns(token);
    }

    public void end(String token){
        endActivitiy(token);
    }

    private synchronized void sendToApns(String token) {

        log.info("send live activity update: " + StringUtils.left(token, 10) + "...");
        LiveActivityModel model = pushService.getActiveLiveActivities().get(token);
        boolean highPriority = model.getHighPriorityCount() == 0 || LocalTime.now().getMinute() % 5 == 0;
        //noinspection UnnecessaryUnicodeEscape
        pushService.sendLiveActivityToApns(token, highPriority, false, buildContentStateMap(true));
        if(highPriority){
            model.setHighPriorityCount(model.getHighPriorityCount() + 1);
        }
    }

    private Map<String, Object> buildContentStateMap(boolean withLiveValue){

        SingleState primaryObject;
        SingleState secondaryObject;

        if(withLiveValue){
            primaryObject = singleStateTime();
            secondaryObject = singleStateDate();
        }else{
            primaryObject = singleStateEmpty();
            secondaryObject = singleStateEmpty();
        }

        Map<String, Object> contentState = new LinkedHashMap<>();
        contentState.put("contentId", UUID.randomUUID().toString());
        contentState.put("timestamp", LocalTime.now().format(DateTimeFormatter.ISO_TIME));
        contentState.put("dismissSeconds", "600"); // FIXME
        contentState.put("primary", buildSingleStateMap(primaryObject));
        contentState.put("secondary", buildSingleStateMap(secondaryObject));

        return contentState;
    }

    private SingleState singleStateEmpty(){
        var state = new SingleState();
        state.val = "--";
        state.symbolName = "square.dashed";
        state.symbolType = "sys";
        state.color = ".grey";
        return state;
    }

    private SingleState singleStateTime(){
        var state = new SingleState();
        state.val = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN).format(LocalDateTime.now());
        state.valShort = state.val;
        state.symbolName = "clock";
        state.symbolType = "sys";
        state.color = ".green";
        return state;
    }

    private SingleState singleStateDate(){
        var state = new SingleState();
        state.val = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN).format(LocalDateTime.now());
        state.valShort = state.val;
        state.symbolName = "calendar";
        state.symbolType = "sys";
        state.color = ".white";
        return state;
    }

    private Map<String, Object> buildSingleStateMap(SingleState state){
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
}
