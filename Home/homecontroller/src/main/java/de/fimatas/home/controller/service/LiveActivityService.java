package de.fimatas.home.controller.service;

import de.fimatas.home.controller.model.LiveActivityModel;
import lombok.extern.apachecommons.CommonsLog;
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

    private final Map<String, LiveActivityModel> activeLiveActivities = new HashMap<>();

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

    public void start(String token, String user, String device){

        if(activeLiveActivities.containsKey(token)){
            return;
        }

        activeLiveActivities.values().stream().filter(la -> la.getUsername().equals(user) && la.getDevice().equals(device))
                .findFirst().ifPresent(la -> end(la.getToken()));

        var model = new LiveActivityModel();
        model.setToken(token);
        model.setUsername(user);
        model.setDevice(device);
        activeLiveActivities.put(token, model);
        updateValue(token);
    }

    public void end(String token){
        endActivitiy(token);
    }

    private void updateValue(String token) {
        sendToApns(token);
    }

    private void sendToApns(String token) {
        boolean highPriority = LocalTime.now().getMinute() % 2 == 0;
        pushService.sendLiveActivityToApns(token, highPriority, false, buildContentStateMap(lookupValue() + " " + highPriority));
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
            pushService.sendLiveActivityToApns(token, true, true, buildContentStateMap("-"));
        } catch (Exception e) {
            log.warn("Could not end LiveActivity via APNS: " + e.getMessage());
        }
        activeLiveActivities.remove(token);
    }
}
