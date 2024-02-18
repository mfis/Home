package de.fimatas.home.client.domain.model;

import de.fimatas.home.library.domain.model.WeatherForecast;
import lombok.Data;

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

    public boolean fitsInSummary(WeatherForecast fc){
        return false; // fromValues.isDay() == fc.isDay();
        // FIXME: && lastDateAdded[0] ==null || !lastDateAdded[0].equals(fc.getTime().toLocalDate())
        // && temperature && rainfall && ...
    }

    public boolean sameDay(WeatherForecast fc){
        return lastProcessedDate!=null && lastProcessedDate.isEqual(fc.getTime().toLocalDate());
    }

    public void integrateInSummary(WeatherForecast fc){
        fromValues = fc;
        toValues = fc; // FIXME!
        lastProcessedDate = fc.getTime().toLocalDate();
    }

    public void reset(){
        fromValues = new WeatherForecast();
        toValues = new WeatherForecast();
    }
}
