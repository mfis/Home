package de.fimatas.home.controller.service;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@CommonsLog
public class WeatherServiceScheduler {

    @Autowired
    private WeatherService weatherService;

    @PostConstruct
    @Scheduled(cron = "2 00 * * * *") // two seconds after full hour
    private void scheduledRefreshHouseModel() {
        try {
            weatherService.refreshWeatherForecastModel();
        }catch(Exception e){
           log.error("Could not call weather service", e);
        }
    }

}
