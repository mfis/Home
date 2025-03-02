package de.fimatas.home.controller.service;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

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
           log.error("Could not call weather service (2-days)", e);
        }
    }

    @Scheduled(cron = "02 03 05,15 * * *")
    public void scheduledRefreshWeatherModelWithFurtherDays() {
        try {
            weatherService.refreshFurtherDaysCache();
        }catch(Exception e){
            log.error("Could not call weather service (further-days)", e);
        }
    }

}
