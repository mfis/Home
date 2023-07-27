package de.fimatas.home.controller.service;

import de.fimatas.home.controller.model.LiveActivityModel;
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
                .findFirst().ifPresent(la -> end(la.getToken()));

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
        pushService.sendLiveActivityToApns(token, highPriority, false, buildContentStateMap(lookupValue() + " " + (highPriority?"\u2191":"\u2193")));
        if(highPriority){
            model.setHighPriorityCount(model.getHighPriorityCount() + 1);
        }
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
            pushService.getActiveLiveActivities().keySet().forEach(this::endActivitiy);
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
        pushService.getActiveLiveActivities().remove(token);
    }
}
