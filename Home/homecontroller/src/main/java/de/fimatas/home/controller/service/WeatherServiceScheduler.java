package de.fimatas.home.controller.service;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@CommonsLog
public class WeatherServiceScheduler {

    @Autowired
    private WeatherService weatherService;

    @PostConstruct
    @Scheduled(cron = "02 05 01-23 * * *")
    public void scheduledRefreshWeatherModel() {
        try {
            weatherService.refreshWeatherForecastModel();
        }catch(Exception e){
           log.error("Could not call weather service(1)", e);
        }
    }

    @Scheduled(cron = "02 03 05,15 * * *")
    public void scheduledRefreshWeatherModelWithFurtherDays() {
        try {
            weatherService.refreshFurtherDaysCache();
        }catch(Exception e){
            log.error("Could not call weather service(2)", e);
        }
    }

}
