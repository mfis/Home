package de.fimatas.home.client.domain.model;

import de.fimatas.home.library.domain.model.WeatherConditions;
import de.fimatas.home.library.domain.model.WeatherForecast;
import de.fimatas.home.library.domain.model.WeatherForecastConclusion;
import de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter;
import lombok.Data;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.LocalDate;

@Data
public class WeatherForecastSummary {

    public WeatherForecastSummary(){
        reset();
    }

    private WeatherForecast fromValues;

    private WeatherForecast toValues;

    private LocalDate lastProcessedDate; // never reset!

    public boolean hasData(){
        return fromValues.getTime() != null && toValues.getTime() != null;
    }

    private static final int TEMPERATURE_RANGE = 2;

    private static final float PRECIPITATION_RANGE = 0.3f;

    private static final float GUST_RANGE = 4f;

    private static final int SUNSHINE_RANGE = 10;

    public boolean fitsInSummary(WeatherForecast fc){
        return fromValues.isDay() == fc.isDay()
                && sameDay(fc)
                && sameIcons(fc)
                && sameConditionColor(fc)
                && temperatureRange(fc)
                && precipitationRange(fc)
                && gustRange(fc)
                && sunshineRange(fc);
    }

    public boolean sameDay(WeatherForecast fc){
        return lastProcessedDate!=null && lastProcessedDate.isEqual(fc.getTime().toLocalDate());
    }

    public void integrateInSummary(WeatherForecast fc){
        // FIXME: clone fc, integrate values
        fromValues = fc;
        toValues = fc; // FIXME!
        lastProcessedDate = fc.getTime().toLocalDate();
    }

    public void reset(){
        fromValues = new WeatherForecast();
        toValues = new WeatherForecast();
    }

    public boolean sameConditionColor(WeatherForecast fc){
        return WeatherForecastConclusionTextFormatter.formatConclusionText(WeatherForecastConclusion.fromWeatherForecast(fromValues))
                .equals(WeatherForecastConclusionTextFormatter.formatConclusionText(WeatherForecastConclusion.fromWeatherForecast(fc)));
    }

    public boolean temperatureRange(WeatherForecast fc){
        var t = new int[]{fromValues.getTemperature().intValue(), toValues.getTemperature().intValue(), fc.getTemperature().intValue()};
        return Math.abs(NumberUtils.min(t) - NumberUtils.max(t)) <= TEMPERATURE_RANGE;
    }

    public boolean precipitationRange(WeatherForecast fc){
        var p = new float[]{fromValues.getPrecipitationInMM().floatValue(), toValues.getPrecipitationInMM().floatValue(), fc.getPrecipitationInMM().floatValue()};
        return Math.abs(NumberUtils.min(p) - NumberUtils.max(p)) <= PRECIPITATION_RANGE;
    }

    public boolean gustRange(WeatherForecast fc){
        if(!fromValues.getIcons().contains(WeatherConditions.WIND) && !fc.getIcons().contains(WeatherConditions.WIND)){
            return true;
        }
        var g = new float[]{fromValues.getGust().floatValue(), toValues.getGust().floatValue(), fc.getGust().floatValue()};
        return Math.abs(NumberUtils.min(g) - NumberUtils.max(g)) <= GUST_RANGE;
    }

    public boolean sunshineRange(WeatherForecast fc){
        var s = new int[]{fromValues.getSunshineInMin().intValue(), toValues.getSunshineInMin().intValue(), fc.getSunshineInMin().intValue()};
        return Math.abs(NumberUtils.min(s) - NumberUtils.max(s)) <= SUNSHINE_RANGE;
    }

    public boolean sameIcons(WeatherForecast fc){
        return fromValues.getIcons().equals(fc.getIcons());
    }
}
