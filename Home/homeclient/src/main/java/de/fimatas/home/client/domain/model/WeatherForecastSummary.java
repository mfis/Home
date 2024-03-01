package de.fimatas.home.client.domain.model;

import de.fimatas.home.library.domain.model.WeatherConditions;
import de.fimatas.home.library.domain.model.WeatherForecast;
import de.fimatas.home.library.domain.model.WeatherForecastConclusion;
import de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter;
import lombok.extern.apachecommons.CommonsLog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@CommonsLog
public class WeatherForecastSummary {

    public WeatherForecastSummary(){
        reset();
    }

    private WeatherForecast fromValues;

    private WeatherForecast toValues;

    private int singleForecastCounter = 0;

    private LocalDate lastProcessedDate; // never reset!

    private static final BigDecimal TEMPERATURE_RANGE = new BigDecimal("1");

    private static final BigDecimal TEMPERATURE_SUMMARY_LIMIT = new BigDecimal("10");

    private static final BigDecimal PRECIPITATION_RANGE = new BigDecimal("0.3");

    private static final BigDecimal GUST_RANGE = new BigDecimal("4");

    private static final BigDecimal SUNSHINE_RANGE = new BigDecimal("8");

    public boolean hasData(){
        return singleForecastCounter > 0;
    }

    public boolean fitsInSummary(WeatherForecast fc){
        return (fromValues == null && toValues == null) ||
                timeRange(fc, false)
                && sameIcons(fc)
                && sameConditionColor(fc)
                && temperatureRange(fc, false)
                && precipitationRange(fc, false)
                && gustRange(fc, false)
                && sunshineRange(fc, false);
    }

    public boolean sameDay(WeatherForecast fc){
        return lastProcessedDate!=null && lastProcessedDate.isEqual(fc.getTime().toLocalDate());
    }

    public void integrateInSummary(WeatherForecast fc){
        lastProcessedDate = fc.getTime().toLocalDate();
        singleForecastCounter++;
        if(fromValues == null || toValues == null) {
            fromValues = new WeatherForecast(fc);
            toValues = new WeatherForecast(fc);
            return;
        }
        timeRange(fc, true);
        // sameIcons(fc); --> ignore
        // sameConditionColor(fc); --> ignore
        temperatureRange(fc, true);
        precipitationRange(fc, true);
        gustRange(fc, true);
        sunshineRange(fc, true);
    }

    public void reset(){
        fromValues = null;
        toValues = null;
        singleForecastCounter = 0;
    }

    public boolean sameConditionColor(WeatherForecast fc){
        return WeatherForecastConclusionTextFormatter.formatConditionColor(WeatherForecastConclusion.fromWeatherForecast(fromValues))
                .equals(WeatherForecastConclusionTextFormatter.formatConditionColor(WeatherForecastConclusion.fromWeatherForecast(fc)));
    }

    public String formatSummaryTimeForView() {
        var hourPattern = DateTimeFormatter.ofPattern("HH");
        if(singleForecastCounter == 1){
            return hourPattern.format(fromValues.getTime()) + " Uhr";
        }else {
            return hourPattern.format(fromValues.getTime()) + ".." + hourPattern.format(toValues.getTime()) + " Uhr je";
        }
    }

    public WeatherForecast getSummary(){
        var summary = new WeatherForecast();
        summary.setTime(null); // --> formatSummaryTimeForView
        summary.setTemperature(fromValues.getTemperature().compareTo(TEMPERATURE_SUMMARY_LIMIT) < 0 ? fromValues.getTemperature() : toValues.getTemperature());
        summary.setWind(toValues.getWind());
        summary.setGust(toValues.getGust());
        summary.setDay(fromValues.isDay());
        summary.setPrecipitationInMM(toValues.getPrecipitationInMM());
        summary.setSunshineInMin(BigDecimal.valueOf(fromValues.getSunshineInMin().intValue() + toValues.getSunshineInMin().intValue() / 2));
        summary.setIcons(fromValues.getIcons());
        return summary;
    }

    private boolean timeRange(WeatherForecast fc, boolean integrate){
        if(integrate){
            var list = List.of(fc.getTime(), fromValues.getTime(), toValues.getTime());
            var min = list.stream().min(LocalDateTime::compareTo).get();
            var max = list.stream().max(LocalDateTime::compareTo).get();
            fromValues.setTime(min);
            toValues.setTime(max);
        }
        var result =  fromValues.isDay() == fc.isDay() && sameDay(fc);
        if(log.isDebugEnabled() &&!result){
            log.debug("not in timeRange " + fc.getTime());
        }
        return result;
    }

    private boolean temperatureRange(WeatherForecast fc, boolean integrate){
        var list = List.of(fc.getTemperature(), fromValues.getTemperature(), toValues.getTemperature());
        var min = list.stream().min(BigDecimal::compareTo).get();
        var max = list.stream().max(BigDecimal::compareTo).get();
        var listRounded = List.of(
                WeatherForecastConclusionTextFormatter.formatTemperature(fc.getTemperature()),
                WeatherForecastConclusionTextFormatter.formatTemperature(fromValues.getTemperature()),
                WeatherForecastConclusionTextFormatter.formatTemperature(toValues.getTemperature())
        );
        var minRounded = listRounded.stream().min(Integer::compareTo).get();
        var maxRounded = listRounded.stream().max(Integer::compareTo).get();
        if(integrate){
            fromValues.setTemperature(min);
            toValues.setTemperature(max);
        }
        var result = (maxRounded - minRounded) <= TEMPERATURE_RANGE.intValue();
        if(log.isDebugEnabled() &&!result) {
            log.debug("not in temperatureRange " + fc.getTime() + "/" + minRounded + "/" + maxRounded);
        }
        return result;
    }

    private boolean precipitationRange(WeatherForecast fc, boolean integrate){
        var list = List.of(fc.getPrecipitationInMM(), fromValues.getPrecipitationInMM(), toValues.getPrecipitationInMM());
        var min = list.stream().min(BigDecimal::compareTo).get();
        var max = list.stream().max(BigDecimal::compareTo).get();
        if(integrate){
            fromValues.setPrecipitationInMM(min);
            toValues.setPrecipitationInMM(max);
        }
        var result =  min.subtract(max).abs().compareTo(PRECIPITATION_RANGE) <= 0;
        if(log.isDebugEnabled() &&!result) {
            log.debug("not in precipitationRange " + fc.getTime());
        }
        return result;
    }

    private boolean gustRange(WeatherForecast fc, boolean integrate){
        var listGust = List.of(fc.getGust(), fromValues.getGust(), toValues.getGust());
        var minGust = listGust.stream().min(BigDecimal::compareTo).get();
        var maxGust = listGust.stream().max(BigDecimal::compareTo).get();
        if(integrate){
            fromValues.setGust(minGust);
            toValues.setGust(maxGust);
            {
                var listWind = List.of(fc.getGust(), fromValues.getGust(), toValues.getGust());
                var minWind = listWind.stream().min(BigDecimal::compareTo).get();
                var maxWind = listWind.stream().max(BigDecimal::compareTo).get();
                fromValues.setWind(minWind);
                toValues.setWind(maxWind);
            }
        }
        if(!fromValues.getIcons().contains(WeatherConditions.WIND) && !fc.getIcons().contains(WeatherConditions.WIND)){
            return true;
        }
        var result =  minGust.subtract(maxGust).abs().compareTo(GUST_RANGE) <= 0;
        if(log.isDebugEnabled() &&!result) {
            log.debug("not in gustRange " + fc.getTime());
        }
        return result;
    }

    private boolean sunshineRange(WeatherForecast fc, boolean integrate){
        var list = List.of(fc.getSunshineInMin(), fromValues.getSunshineInMin(), toValues.getSunshineInMin());
        var min = list.stream().min(BigDecimal::compareTo).get();
        var max = list.stream().max(BigDecimal::compareTo).get();
        if(integrate){
            fromValues.setSunshineInMin(min);
            toValues.setSunshineInMin(max);
        }
        var result =   min.subtract(max).abs().compareTo(SUNSHINE_RANGE) <= 0;
        if(log.isDebugEnabled() &&!result) {
            log.debug("not in sunshineRange " + fc.getTime() + " " + fromValues.getSunshineInMin() + "/" + toValues.getSunshineInMin() + "/" + fc.getSunshineInMin());
        }
        return result;
    }

    private boolean sameIcons(WeatherForecast fc){
        return fromValues.getIcons().equals(fc.getIcons());
    }
}
