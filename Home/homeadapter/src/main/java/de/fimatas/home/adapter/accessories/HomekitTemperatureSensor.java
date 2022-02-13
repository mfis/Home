package de.fimatas.home.adapter.accessories;

import de.fimatas.home.adapter.model.HomekitAccessoryWithModelField;
import de.fimatas.home.library.domain.model.Climate;
import io.github.hapjava.accessories.TemperatureSensorAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import lombok.extern.apachecommons.CommonsLog;

import java.util.concurrent.CompletableFuture;

@CommonsLog
public class HomekitTemperatureSensor extends HomekitAccessoryWithModelField implements TemperatureSensorAccessory {

    public double getMinCurrentTemperature() {
        return -100.0D;
    }

    public CompletableFuture<String> getModel() {
        return CompletableFuture.completedFuture("TemperatureSensor");
    }

    @Override
    protected Object actualValue() {
        final Climate climate = (Climate) lookupDeviceModel(false);
        if(climate != null && climate.getTemperature() != null){
            return climate.getTemperature().getValue().doubleValue();
        }else{
            return null;
        }
    }

    @Override
    public void subscribeCurrentTemperature(HomekitCharacteristicChangeCallback homekitCharacteristicChangeCallback) {
        subscribe(homekitCharacteristicChangeCallback);
    }

    @Override
    public void unsubscribeCurrentTemperature() {
        unsubscribe();
    }

    @Override
    public CompletableFuture<Double> getCurrentTemperature() {
        return CompletableFuture.completedFuture((Double) actualValue());
    }

    @Override
    public CompletableFuture<String> getName() {
        return CompletableFuture.completedFuture("Temperatur " + lookupPlaceName());
    }

}
