package de.fimatas.home.library.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EnablePhotovoltaicsOverflow {
    String shortName();
    int defaultWattage();
    int percentageMaxPowerFromGrid();
    int switchOnDelay();
    int switchOffDelay();
    int defaultPriority();
}