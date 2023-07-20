package de.fimatas.home.controller.service;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@CommonsLog
public class LiveActivityService {

    @Autowired
    private PushService pushService;

    private final static int INVALIDATION_MINUTES = 15;

    private final Map<String, Object> activeLiveActivities = new HashMap<>();

    private boolean highPriorityToggle = false; // FIXME: token-level

    @Scheduled(cron = "0 * * * * *")
    public void run(){
        activeLiveActivities.keySet().forEach(this::updateValue);
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void endAll(){
        endAllActivities();
    }

    @PreDestroy
    public void preDestroy(){
        endAllActivities();
    }

    public void start(String token){
        if(activeLiveActivities.containsKey(token)){
            return;
        }
        activeLiveActivities.put(token, new Object());
        highPriorityToggle = true;
        updateValue(token);
    }

    public void end(String token){
        endActivitiy(token);
    }

    private void updateValue(String token) {
        sendToApns(token);
    }

    private void sendToApns(String token) {
        pushService.sendLiveActivityToApns(token, highPriorityToggle, INVALIDATION_MINUTES, false, buildContentStateMap(lookupValue() + " " + highPriorityToggle));
        highPriorityToggle = !highPriorityToggle;
    }

    private Map<String, Object> buildContentStateMap(String value){

        Map<String, Object> contentState = new LinkedHashMap<>();

        contentState.put("valueLeading", value);
        contentState.put("colorLeading", ".green");

        contentState.put("valueTrailing", "");
        contentState.put("colorTrailing", "");

        return contentState;
    }

    private String lookupValue(){
        return DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN).format(LocalDateTime.now());
    }


    private void endAllActivities() {
        try {
            activeLiveActivities.keySet().forEach(this::endActivitiy);
        } catch (Exception e) {
            log.warn("Could not end LiveActivity via APNS");
        }
    }

    private void endActivitiy(String token) {
        try {
            pushService.sendLiveActivityToApns(token, true, 1, true, buildContentStateMap("-"));
        } catch (Exception e) {
            log.warn("Could not end LiveActivity via APNS: " + e.getMessage());
        }
        activeLiveActivities.remove(token);
    }
}
