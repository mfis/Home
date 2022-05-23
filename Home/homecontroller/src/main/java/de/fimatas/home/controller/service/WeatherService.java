package de.fimatas.home.controller.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import de.fimatas.home.controller.api.BrightSkyAPI;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.WeatherConditions;
import de.fimatas.home.library.domain.model.WeatherForecast;
import de.fimatas.home.library.domain.model.WeatherForecastConclusion;
import de.fimatas.home.library.domain.model.WeatherForecastModel;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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

    @Retryable(value = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 5000))
    public void refreshWeatherForecastModel() {

        var model = new WeatherForecastModel();
        model.setForecasts(mapApiResponse(brightSkyAPI.call()));
        model.setDateTime(System.currentTimeMillis());
        model.setSourceText(env.getProperty("weatherForecast.sourcetext"));

        calculateSunriseSunset(model.getForecasts());
        calculateConclusions(model);

        ModelObjectDAO.getInstance().write(model);
        uploadService.uploadToClient(model);
    }

    private List<WeatherForecast> mapApiResponse(List<JsonNode> jsonNodes) {

        List<WeatherForecast> forecasts = new LinkedList<>();
        jsonNodes.forEach(entry -> {

            LocalDateTime dateTime = LocalDateTime.parse(entry.get("timestamp").asText(), ISO_OFFSET_DATE_TIME);

            if (dateTime.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS))) {
                return;
            }

            if (forecasts.stream().anyMatch(wf -> wf.getTime().equals(dateTime))) {
                return;
            }

            var condition = BrightSkyCondition.fromValueString(entry.get("condition").asText());
            var icon = BrightSkyIcon.fromValueString(entry.get("icon").asText());

            var forecast = new WeatherForecast();
            forecast.setTime(dateTime);
            forecast.setTemperature(new BigDecimal(entry.get("temperature").asText()));
            forecast.setWind(new BigDecimal(entry.get("wind_speed").asText()));
            forecast.setIcons(addConditions(condition, icon, forecast.getWind()));
            forecasts.add(forecast);
        });
        return forecasts;
    }

    private List<WeatherConditions> addConditions(BrightSkyCondition condition, BrightSkyIcon icon, BigDecimal windSpeed) {

        var icons = new LinkedList<WeatherConditions>();
        if (condition == null || icon == null) {
            icons.add(WeatherConditions.UNKNOWN);
            log.warn("Unknown condition/icon:" + condition + "/" + icon);
            return icons;
        }

        switch (icon) {
            case CLEAR_DAY:
                icons.add(WeatherConditions.SUN);
                break;
            case CLEAR_NIGHT:
                icons.add(WeatherConditions.MOON);
                break;
            case PARTLY_CLOUDY_DAY:
                icons.add(WeatherConditions.SUN_CLOUD);
                break;
            case PARTLY_CLOUDY_NIGHT:
                icons.add(WeatherConditions.MOON_CLOUD);
                break;
            case CLOUDY:
                if (condition == BrightSkyCondition.RAIN) {
                    icons.add(WeatherConditions.CLOUD_RAIN);
                } else {
                    icons.add(WeatherConditions.CLOUD);
                }
                break;
            case FOG:
                icons.add(WeatherConditions.FOG);
                break;
            case WIND:
                icons.add(WeatherConditions.WIND);
                break;
            case RAIN:
                icons.add(WeatherConditions.RAIN);
                break;
            case SLEET:
            case SNOW:
                icons.add(WeatherConditions.SNOW);
                break;
            case HAIL:
                icons.add(WeatherConditions.HAIL);
                break;
            case THUNDERSTORM:
                icons.add(WeatherConditions.THUNDERSTORM);
                break;
            default:
                break;
        }

        if (condition == BrightSkyCondition.SNOW && !icons.contains(WeatherConditions.SNOW)) {
            icons.remove(WeatherConditions.RAIN);
            icons.remove(WeatherConditions.CLOUD_RAIN);
            icons.remove(WeatherConditions.CLOUD);
            icons.remove(WeatherConditions.SUN);
            icons.remove(WeatherConditions.SUN_CLOUD);
            icons.remove(WeatherConditions.MOON);
            icons.remove(WeatherConditions.MOON_CLOUD);
            icons.add(WeatherConditions.SNOW);
        }

        if (windSpeed != null && windSpeed.compareTo(WIND_SPEED_STORM) > 0 && !icons.contains(WeatherConditions.WIND)) {
            icons.add(WeatherConditions.WIND);
        }

        if (condition == BrightSkyCondition.RAIN && !icons.contains(WeatherConditions.CLOUD_RAIN) && !icons.contains(WeatherConditions.RAIN) && !icons.contains(WeatherConditions.SNOW)) {
            icons.remove(WeatherConditions.SUN);
            icons.remove(WeatherConditions.SUN_CLOUD);
            icons.remove(WeatherConditions.MOON);
            icons.remove(WeatherConditions.MOON_CLOUD);
            icons.add(0, WeatherConditions.RAIN);
        }

        return icons;
    }

    private void calculateConclusions(WeatherForecastModel model) {

        if(model.getForecasts().isEmpty() || model.getForecasts().stream().anyMatch(fc -> fc.getTemperature() == null || fc.getWind() == null)){
            return;
        }

        model.setConclusion24to48hours(calculateConclusionForTimerange(model.getForecasts()));
        model.setConclusionToday(calculateConclusionForTimerange(model.getForecasts().stream().filter(fc -> fc.getTime().toLocalDate().isEqual(LocalDate.now())).collect(Collectors.toList())));
        model.setConclusionTomorrow(calculateConclusionForTimerange(model.getForecasts().stream().filter(fc -> fc.getTime().toLocalDate().isEqual(LocalDate.now().plusDays(1))).collect(Collectors.toList())));

        model.getConclusionForDate().put(LocalDate.now(), model.getConclusionToday());
        model.getConclusionForDate().put(LocalDate.now().plusDays(1), model.getConclusionTomorrow());
    }

    static WeatherForecastConclusion calculateConclusionForTimerange(List<WeatherForecast> items) {

        var conclusion = new WeatherForecastConclusion();
        conclusion.setConditions(new LinkedList<>());

        conclusion.setMinTemp(items.stream().map(WeatherForecast::getTemperature).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null));
        conclusion.setMaxTemp(items.stream().map(WeatherForecast::getTemperature).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null));
        conclusion.setMaxWind(items.stream().filter(fc -> fc.getWind()!=null).map(fc -> fc.getWind().setScale(0, RoundingMode.HALF_UP).intValue()).max(Integer::compare).orElse(null));

        final Optional<WeatherForecast> firstSnow = items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.SNOW)).findFirst();
        firstSnow.ifPresent(weatherForecast -> addConclusionWeatherContition(conclusion, weatherForecast, WeatherConditions.SNOW));

        final Optional<WeatherForecast> firstWind = items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.WIND)).findFirst();
        firstWind.ifPresent(weatherForecast -> addConclusionWeatherContition(conclusion, weatherForecast, WeatherConditions.WIND));

        final Optional<WeatherForecast> firstHail = items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.HAIL)).findFirst();
        firstHail.ifPresent(weatherForecast -> addConclusionWeatherContition(conclusion, weatherForecast, WeatherConditions.HAIL));

        final Optional<WeatherForecast> firstThunderstorm = items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.THUNDERSTORM)).findFirst();
        firstThunderstorm.ifPresent(weatherForecast -> addConclusionWeatherContition(conclusion, weatherForecast, WeatherConditions.THUNDERSTORM));

        final Optional<WeatherForecast> firstKindOfRain = items.stream().filter(fc -> fc.getIcons().stream().anyMatch(WeatherConditions::isKindOfRain)).findFirst();
        if(items.stream().anyMatch(fc -> fc.getIcons().stream().anyMatch(WeatherConditions::isKindOfRain))){
            if(!conclusion.getConditions().contains(WeatherConditions.HAIL) && !conclusion.getConditions().contains(WeatherConditions.THUNDERSTORM)){
                addConclusionWeatherContition(conclusion, firstKindOfRain.orElse(null), WeatherConditions.RAIN);
            }
        }

        if(items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.SUN)).count() > 2){
            addConclusionWeatherContition(conclusion, items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.SUN)).findFirst().orElse(null), WeatherConditions.SUN);
        }

        if(items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.SUN) || fc.getIcons().contains(WeatherConditions.SUN_CLOUD)).count()>3){
            addConclusionWeatherContition(conclusion, items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.SUN) || fc.getIcons().contains(WeatherConditions.SUN_CLOUD)).findFirst().orElse(null), WeatherConditions.SUN_CLOUD);
        }

        return conclusion;
    }

    private static void addConclusionWeatherContition(WeatherForecastConclusion conclusion, WeatherForecast forecast, WeatherConditions newCondition){
        conclusion.getConditions().add(newCondition);
        if(!conclusion.getFirstOccurences().containsKey(newCondition) && forecast != null){
            conclusion.getFirstOccurences().put(newCondition, forecast.getTime());
        }
    }

    private void calculateSunriseSunset(List<WeatherForecast> forecasts) {

        LocalDate lastDateProcessed = null;
        LocalDateTime actualSunrise = null;
        LocalDateTime actualSunset = null;

        for(var forecast: forecasts){
            if(lastDateProcessed == null || !forecast.getTime().toLocalDate().isEqual(lastDateProcessed)){
                Location location = new Location(Objects.requireNonNull(env.getProperty("weatherForecast.lat")), Objects.requireNonNull(env.getProperty("weatherForecast.lon")));
                SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, env.getProperty("weatherForecast.timezone"));
                Calendar targetCalendar = GregorianCalendar.from(forecast.getTime().toLocalDate().atStartOfDay(ZoneId.systemDefault()));
                Calendar actualSunriseCalendar = calculator.getOfficialSunriseCalendarForDate(targetCalendar);
                Calendar actualSunsetCalendar = calculator.getOfficialSunsetCalendarForDate(targetCalendar);
                actualSunrise = LocalDateTime.ofInstant(actualSunriseCalendar.toInstant(), actualSunriseCalendar.getTimeZone().toZoneId());
                actualSunset = LocalDateTime.ofInstant(actualSunsetCalendar.toInstant(), actualSunsetCalendar.getTimeZone().toZoneId());
                lastDateProcessed = forecast.getTime().toLocalDate();
            }
            forecast.setDay(forecast.getTime().getHour()>actualSunrise.getHour() && forecast.getTime().getHour()<actualSunset.getHour());
        }
    }

    enum BrightSkyCondition {
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

    enum BrightSkyIcon {
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
