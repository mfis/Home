package de.fimatas.home.controller.service;

import com.fasterxml.jackson.databind.JsonNode;
import de.fimatas.home.controller.api.BrightSkyAPI;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.WeatherForecast;
import de.fimatas.home.library.domain.model.WeatherForecastModel;
import de.fimatas.home.library.domain.model.WeatherIcons;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@Component
@CommonsLog
public class WeatherService {

    @Autowired
    private BrightSkyAPI brightSkyAPI;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private Environment env;

    private static final BigDecimal WIND_SPEED_STORM = BigDecimal.valueOf(35);

    @Retryable( value = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 5000))
    public void refreshWeatherForecastModel() {

        final List<JsonNode> jsonNodes = brightSkyAPI.call();
        List<WeatherForecast> forecasts = new LinkedList<>();

        jsonNodes.forEach(entry -> {

            LocalDateTime dateTime = LocalDateTime.parse(entry.get("timestamp").asText(), ISO_OFFSET_DATE_TIME);

            if(dateTime.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS))){
                return;
            }

            if(forecasts.stream().anyMatch(wf -> wf.getTime().equals(dateTime))){
                return;
            }

            var condition = BrightSkyCondition.fromValueString(entry.get("condition").asText());
            var icon = BrightSkyIcon.fromValueString(entry.get("icon").asText());

            var forecast = new WeatherForecast();
            forecast.setTime(dateTime);
            forecast.setTemperature(new BigDecimal(entry.get("temperature").asText()));
            forecast.setWind(new BigDecimal(entry.get("wind_speed").asText()));
            forecast.setIcons(internalIconNames(condition, icon, forecast.getWind()));
            forecasts.add(forecast);
        });

        var model = new WeatherForecastModel();
        model.setDateTime(System.currentTimeMillis());
        model.setSourceText(env.getProperty("weatherForecast.sourcetext"));
        model.setForecasts(forecasts);
        ModelObjectDAO.getInstance().write(model);
        uploadService.uploadToClient(model);
    }

    private List<WeatherIcons> internalIconNames(BrightSkyCondition condition, BrightSkyIcon icon, BigDecimal windSpeed) {

        var icons = new LinkedList<WeatherIcons>();
        if(condition == null || icon == null){
            icons.add(WeatherIcons.UNKNOWN);
            log.warn("Unknown condition/icon:" + condition + "/" + icon);
            return icons;
        }

        switch (icon){
            case CLEAR_DAY:
                icons.add(WeatherIcons.SUN);
                break;
            case CLEAR_NIGHT:
                icons.add(WeatherIcons.MOON);
                break;
            case PARTLY_CLOUDY_DAY:
                icons.add(WeatherIcons.SUN_CLOUD);
                break;
            case PARTLY_CLOUDY_NIGHT:
                icons.add(WeatherIcons.MOON_CLOUD);
                break;
            case CLOUDY:
                if(condition==BrightSkyCondition.RAIN){
                    icons.add(WeatherIcons.CLOUD_RAIN);
                }else{
                    icons.add(WeatherIcons.CLOUD);
                }
                break;
            case FOG:
                icons.add(WeatherIcons.FOG);
                break;
            case WIND:
                icons.add(WeatherIcons.WIND);
                break;
            case RAIN:
                icons.add(WeatherIcons.RAIN);
                break;
            case SLEET:
            case SNOW:
                icons.add(WeatherIcons.SNOW);
                break;
            case HAIL:
                icons.add(WeatherIcons.HAIL);
                break;
            case THUNDERSTORM:
                icons.add(WeatherIcons.THUNDERSTORM);
                break;
            default:
                break;
        }

        if (condition== BrightSkyCondition.SNOW && !icons.contains(WeatherIcons.SNOW)){
            icons.remove(WeatherIcons.RAIN);
            icons.remove(WeatherIcons.CLOUD_RAIN);
            icons.remove(WeatherIcons.CLOUD);
            icons.remove(WeatherIcons.SUN);
            icons.remove(WeatherIcons.SUN_CLOUD);
            icons.remove(WeatherIcons.MOON);
            icons.remove(WeatherIcons.MOON_CLOUD);
            icons.add(WeatherIcons.SNOW);
        }

        if(windSpeed!=null && windSpeed.compareTo(WIND_SPEED_STORM) > 0 && !icons.contains(WeatherIcons.WIND)){
            icons.add(WeatherIcons.WIND);
        }

        if (condition == BrightSkyCondition.RAIN && !icons.contains(WeatherIcons.CLOUD_RAIN) && !icons.contains(WeatherIcons.RAIN) && !icons.contains(WeatherIcons.SNOW)){
            icons.remove(WeatherIcons.SUN);
            icons.remove(WeatherIcons.SUN_CLOUD);
            icons.remove(WeatherIcons.MOON);
            icons.remove(WeatherIcons.MOON_CLOUD);
            icons.add(0, WeatherIcons.RAIN);
        }

        return icons;
    }

    enum BrightSkyCondition{
        DRY, FOG, RAIN, SLEET, SNOW, HAIL, THUNDERSTORM, NULL;
        public static BrightSkyCondition fromValueString(String string) {
            for (BrightSkyCondition value : values()) {
                if (value.name().equals(string.replace('-', '_').toUpperCase())) {
                    return value;
                }
            }
            return null;
        }
    }

    enum BrightSkyIcon{
        CLEAR_DAY, CLEAR_NIGHT, PARTLY_CLOUDY_DAY, PARTLY_CLOUDY_NIGHT, CLOUDY, FOG, WIND, RAIN, SLEET, SNOW, HAIL, THUNDERSTORM, NULL;
        public static BrightSkyIcon fromValueString(String string) {
            for (BrightSkyIcon value : values()) {
                if (value.name().equals(string.replace('-', '_').toUpperCase())) {
                    return value;
                }
            }
            return null;
        }
    }
}
