package de.fimatas.home.controller.service;

import de.fimatas.home.controller.api.ExternalServiceHttpAPI;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import org.springframework.web.client.RestClientException;

@Component
@CommonsLog
public class WeatherServiceScheduler {

    @Autowired
    private WeatherService weatherService;

    @PostConstruct
    public void init(){
        scheduledRefreshWeatherModelWithFurtherDays();
        scheduledRefreshWeatherModel();
    }

    @Scheduled(cron = "02 05 04-23 * * *")
    public void scheduledRefreshWeatherModel() {
        try {
            weatherService.refreshWeatherForecastModel();
        }catch(Exception e){
            handleException(e, "Could not call weather service (2-days)");
        }
    }

    @Scheduled(cron = "02 03 05,15 * * *")
    public void scheduledRefreshWeatherModelWithFurtherDays() {
        try {
            weatherService.refreshFurtherDaysCache();
        }catch(Exception e){
            handleException(e, "Could not call weather service (further-days)");
        }
    }

    private static void handleException(Exception e, String msg) {
        if(e instanceof RestClientException && e.getMessage().startsWith(ExternalServiceHttpAPI.MESSAGE_TOO_MANY_CALLS)){
            log.warn(msg + e.getMessage());
            return;
        }
        log.error(msg, e);
    }
}
