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
    @Scheduled(cron = "2 00 1-23 * * *") // two seconds after full hour
    private void scheduledRefreshWeatherModel() {
        try {
            weatherService.refreshWeatherForecastModel();
        }catch(Exception e){
           log.error("Could not call weather service(1)", e);
        }
    }

    @PostConstruct
    @Scheduled(cron = "2 00 0 * * *") // two seconds after full hour
    private void scheduledRefreshWeatherModelWithFurtherDays() {
        try {
            weatherService.refreshFurtherDaysCache();
            weatherService.refreshWeatherForecastModel();
        }catch(Exception e){
            log.error("Could not call weather service(2)", e);
        }
    }

}
