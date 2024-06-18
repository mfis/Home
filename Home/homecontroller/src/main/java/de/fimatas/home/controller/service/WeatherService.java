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
import de.fimatas.home.library.util.HomeUtils;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
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

    private static final BigDecimal WIND_SPEED_STORM = BigDecimal.valueOf(36);
    private static final BigDecimal WIND_SPEED_GUST_STORM = BigDecimal.valueOf(70);

    private static final BigDecimal HEAVY_RAIN_MM = BigDecimal.valueOf(10);

    @Retryable(retryFor = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 5000))
    public void refreshFurtherDaysCache() {
        brightSkyAPI.cachingCallForFurtherDays();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 5000))
    public void refreshWeatherForecastModel() {

        var model = new WeatherForecastModel();
        model.setForecasts(mapApiResponse(brightSkyAPI.callTwoDays()));
        model.setDateTime(System.currentTimeMillis());
        model.setSourceText(env.getProperty("weatherForecast.sourcetext"));

        calculateSunriseSunset(model.getForecasts());
        calculateConclusions(model);

        final Map<LocalDate, List<JsonNode>> furtherDays = brightSkyAPI.getCachedFurtherDays();
        furtherDays.forEach((date, day) -> {
            final List<WeatherForecast> dayForecasts = mapApiResponse(day);
            final WeatherForecastConclusion dayConclusion = calculateConclusionForTimerange(dayForecasts);
            WeatherConditions.lessSignificantConditions().forEach(c -> {
                if(dayConclusion.getConditions().isEmpty()) {
                    dayForecasts.stream().filter(fc -> fc.getIcons().contains(c)).findFirst().ifPresent(i -> dayConclusion.getConditions().add(c));
                }
                });
            model.getFurtherDays().put(date, dayConclusion);
        });

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
            forecast.setTemperature(nodeToBigDecimal(entry.get("temperature")));
            forecast.setWind(nodeToBigDecimal(entry.get("wind_speed")));
            forecast.setGust(nodeToBigDecimal(entry.get("wind_gust_speed")));
            forecast.setPrecipitationInMM(nodeToBigDecimal(entry.get("precipitation")));
            forecast.setPrecipitationProbability(HomeUtils.roundPercentageToNearestTen(nodeToInteger(entry.get("precipitation_probability"))));
            forecast.setSunshineInMin(nodeToBigDecimal(entry.get("sunshine")));
            forecast.setIcons(addConditions(condition, icon, forecast));
            forecasts.add(forecast);
        });
        return forecasts;
    }

    private BigDecimal nodeToBigDecimal(JsonNode node){
        if(node == null || StringUtils.isBlank(node.asText())){
            return null;
        }
        return new BigDecimal(node.asText());
    }

    private Integer nodeToInteger(JsonNode node){
        if(node == null || StringUtils.isBlank(node.asText())){
            return null;
        }
        return Integer.parseInt(node.asText());
    }

    private Set<WeatherConditions> addConditions(BrightSkyCondition condition, BrightSkyIcon icon, WeatherForecast forecast) {

        var icons = new LinkedHashSet<WeatherConditions>();
        if (condition == null || icon == null) {
            icons.add(WeatherConditions.UNKNOWN);
            log.warn("Unknown condition/icon:" + condition + "/" + icon);
            return icons;
        }

        switch (icon) {
            case CLEAR_DAY -> icons.add(WeatherConditions.SUN);
            case CLEAR_NIGHT -> icons.add(WeatherConditions.MOON);
            case PARTLY_CLOUDY_DAY -> icons.add(WeatherConditions.SUN_CLOUD);
            case PARTLY_CLOUDY_NIGHT -> icons.add(WeatherConditions.MOON_CLOUD);
            case CLOUDY -> {
                if (condition == BrightSkyCondition.RAIN) {
                    icons.add(WeatherConditions.RAIN);
                } else {
                    icons.add(WeatherConditions.CLOUD);
                }
            }
            case FOG -> icons.add(WeatherConditions.FOG);
            case WIND -> icons.add(WeatherConditions.WIND);
            case RAIN -> icons.add(WeatherConditions.RAIN);
            case SLEET, SNOW -> {
                icons.add(WeatherConditions.SNOW);
                icons.remove(WeatherConditions.RAIN);
                icons.remove(WeatherConditions.HEAVY_RAIN);
            }
            case HAIL -> icons.add(WeatherConditions.HAIL);
            case THUNDERSTORM -> icons.add(WeatherConditions.THUNDERSTORM);
            default -> {
            }
        }

        if (forecast.getWind() != null && forecast.getWind().compareTo(WIND_SPEED_STORM) > 0) {
            icons.add(WeatherConditions.WIND);
        }

        if (forecast.getGust() != null && forecast.getGust().compareTo(WIND_SPEED_GUST_STORM) > 0) {
            icons.add(WeatherConditions.GUST);
        }

        if(forecast.getPrecipitationInMM().compareTo(BigDecimal.ZERO) == 0){ // not showing "0.0mm rain"
            if(icons.stream().anyMatch(i -> Set.of(WeatherConditions.SNOW, WeatherConditions.HEAVY_RAIN, WeatherConditions.RAIN, WeatherConditions.HAIL).contains(i))){
                forecast.setPrecipitationInMM(new BigDecimal("0.1"));
            }
        }

        if(forecast.getPrecipitationInMM().compareTo(HEAVY_RAIN_MM) > 0){
            icons.remove(WeatherConditions.RAIN);
            icons.add(WeatherConditions.HEAVY_RAIN);
        }

        if(forecast.getSunshineInMin().compareTo(BigDecimal.ZERO) == 0){ // not showing "0 minutes sun"
            if(icons.stream().anyMatch(i -> Set.of(WeatherConditions.SUN_CLOUD, WeatherConditions.SUN).contains(i))){
                icons.remove(WeatherConditions.SUN_CLOUD);
                icons.remove(WeatherConditions.SUN);
                icons.add(WeatherConditions.CLOUD);
            }
        }

        return reduceConditions(icons);
    }

    private static Set<WeatherConditions> reduceConditions(Set<WeatherConditions> set){

        if (set.contains(WeatherConditions.GUST)) {
            set.remove(WeatherConditions.WIND);
        }

        if (set.contains(WeatherConditions.HEAVY_RAIN)) {
            set.remove(WeatherConditions.RAIN);
            set.remove(WeatherConditions.SUN);
            set.remove(WeatherConditions.SUN_CLOUD);
            set.remove(WeatherConditions.MOON);
            set.remove(WeatherConditions.MOON_CLOUD);
        }

        return set.stream()
                .sorted(Comparator.comparing(WeatherConditions::ordinalAsInteger))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void calculateConclusions(WeatherForecastModel model) {

        if(model.getForecasts().isEmpty() || model.getForecasts().stream().anyMatch(fc -> fc.getTemperature() == null || fc.getWind() == null)){
            return;
        }

        model.setConclusion24to48hours(calculateConclusionForTimerange(model.getForecasts()));
        model.setConclusionToday(calculateConclusionForTimerange(model.getForecasts().stream().filter(fc -> fc.getTime().toLocalDate().isEqual(LocalDate.now())).collect(Collectors.toList())));
        model.setConclusionTomorrow(calculateConclusionForTimerange(model.getForecasts().stream().filter(fc -> fc.getTime().toLocalDate().isEqual(LocalDate.now().plusDays(1))).collect(Collectors.toList())));
        model.setConclusion3hours(calculateConclusionForTimerange(model.getForecasts().stream().filter(fc -> fc.getTime().isBefore(LocalDateTime.now().plusHours(3))).collect(Collectors.toList())));

        model.getConclusionForDate().put(LocalDate.now(), model.getConclusionToday());
        model.getConclusionForDate().put(LocalDate.now().plusDays(1), model.getConclusionTomorrow());
    }

    static WeatherForecastConclusion calculateConclusionForTimerange(List<WeatherForecast> items) {

        var conclusion = new WeatherForecastConclusion();
        conclusion.setConditions(new LinkedHashSet<>());

        conclusion.setMinTemp(items.stream().map(WeatherForecast::getTemperature).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null));
        conclusion.setMaxTemp(items.stream().map(WeatherForecast::getTemperature).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null));
        conclusion.setMaxWind(items.stream().filter(fc -> fc.getWind()!=null).map(fc -> fc.getWind().setScale(0, RoundingMode.HALF_UP).intValue()).max(Integer::compare).orElse(null));
        conclusion.setMaxGust(items.stream().filter(fc -> fc.getGust()!=null).map(fc -> fc.getGust().setScale(0, RoundingMode.HALF_UP).intValue()).max(Integer::compare).orElse(null));
        conclusion.setPrecipitationInMM(items.stream().map(WeatherForecast::getPrecipitationInMM).reduce(BigDecimal.ZERO, BigDecimal::add));
        conclusion.setPrecipitationProbability(items.stream().map(WeatherForecast::getPrecipitationProbability).filter(Objects::nonNull).max(Integer::compareTo).orElse(null));
        conclusion.setSunshineInMin(items.stream().map(WeatherForecast::getSunshineInMin).reduce(BigDecimal.ZERO, BigDecimal::add));

        Arrays.stream(WeatherConditions.values()).forEach(c -> {
            final Optional<WeatherForecast> first = items.stream().filter(fc -> fc.getIcons().contains(c)).findFirst();
            boolean add = c != WeatherConditions.SUN || items.stream().filter(fc -> fc.getIcons().contains(WeatherConditions.SUN)).count() >= 3;
            if(add){
                first.ifPresent(weatherForecast -> addConclusionWeatherContition(conclusion, weatherForecast, c));
            }
        });

        conclusion.setConditions(reduceConditions(conclusion.getConditions()));
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

            boolean sunsetDay = (forecast.getTime().getHour()==actualSunrise.getHour() && actualSunrise.getMinute() < 30)
                    || forecast.getTime().getHour()>actualSunrise.getHour();
            boolean sunriseDay = (forecast.getTime().getHour()==actualSunset.getHour()
                    && actualSunset.getMinute() > 30)
                    || forecast.getTime().getHour()<actualSunset.getHour();

            forecast.setDay(sunsetDay && sunriseDay);

            // Adjust icons, because weather api can calculate day/night icons with different algorithm
            if(forecast.isDay()){
                replaceCondition(forecast, WeatherConditions.MOON_CLOUD, WeatherConditions.SUN_CLOUD);
                replaceCondition(forecast, WeatherConditions.MOON, WeatherConditions.SUN);
            }
            if(!forecast.isDay()){
                replaceCondition(forecast, WeatherConditions.SUN_CLOUD, WeatherConditions.MOON_CLOUD);
                replaceCondition(forecast, WeatherConditions.SUN, WeatherConditions.MOON);
                forecast.setSunshineInMin(BigDecimal.ZERO); // cut single minutes of sunshine in hours that are listed as night
            }
        }
    }

    private void replaceCondition (WeatherForecast forecast, WeatherConditions from, WeatherConditions to){
        if(forecast.getIcons().contains(from)){
            forecast.getIcons().remove(from);
            forecast.getIcons().add(to);
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
