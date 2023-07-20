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

    private final List<String> activeLiveActivities = new LinkedList<>();

    @Scheduled(cron = "0 * * * * *")
    public void run(){
        activeLiveActivities.forEach(this::updateValue);
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
        activeLiveActivities.add(token);
        updateValue(token);
    }

    public void end(String token){
        endActivitiy(token);
    }

    private void updateValue(String token) {
        pushService.sendLiveActivityToApns(token, true, INVALIDATION_MINUTES, false, buildContentStateMap(lookupValue()));
    }

    private Map<String, Object> buildContentStateMap(String value){

        Map<String, Object> contentState = new LinkedHashMap<>();

        contentState.put("valueLeading", value);
        contentState.put("colorLeading", "green");

        contentState.put("valueTrailing", "");
        contentState.put("colorTrailing", "");

        return contentState;
    }

    private String lookupValue(){
        return DateTimeFormatter.ofPattern("HHmmss", Locale.GERMAN).format(LocalDateTime.now());
    }


    private void endAllActivities() {
        try {
            activeLiveActivities.forEach(this::endActivitiy);
        } catch (Exception e) {
            log.warn("Could not end LiveActivity via APNS");
        }
    }

    private void endActivitiy(String token) {
        try {
            pushService.sendLiveActivityToApns(token, false, 1, true, buildContentStateMap("-"));
        } catch (Exception e) {
            log.warn("Could not end LiveActivity via APNS: " + e.getMessage());
        }
        activeLiveActivities.remove(token);
    }
}
