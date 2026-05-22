package de.fimatas.home.controller.model;

import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.command.PersistentCacheCommand;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.homematic.model.HistoryStrategy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class History {

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    private Map<String, HistoryElement> elements;

    public final static String HIST_THERMOMETER_KINDERZIMMER_1_TEMP = "HIST_THERMOMETER_KINDERZIMMER_1_TEMP";
    public final static String HIST_THERMOMETER_KINDERZIMMER_1_HUM = "HIST_THERMOMETER_KINDERZIMMER_1_HUM";
    public final static String HIST_THERMOMETER_KINDERZIMMER_2_TEMP = "HIST_THERMOMETER_KINDERZIMMER_2_TEMP";
    public final static String HIST_THERMOMETER_KINDERZIMMER_2_HUM = "HIST_THERMOMETER_KINDERZIMMER_2_HUM";
    public final static String HIST_THERMOMETER_SCHLAFZIMMER_1_TEMP = "HIST_THERMOMETER_SCHLAFZIMMER_1_TEMP";
    public final static String HIST_THERMOMETER_SCHLAFZIMMER_1_HUM = "HIST_THERMOMETER_SCHLAFZIMMER_1_HUM";
    public final static String HIST_THERMOMETER_WASCHKUECHE_2_TEMP = "HIST_THERMOMETER_WASCHKUECHE_2_TEMP";
    public final static String HIST_THERMOMETER_WASCHKUECHE_2_HUM = "HIST_THERMOMETER_WASCHKUECHE_2_HUM";
    public final static String HIST_THERMOMETER_AUSSEN_2_TEMP = "HIST_THERMOMETER_AUSSEN_2_TEMP";
    public final static String HIST_STROM_BEZUG = "HIST_STROM_BEZUG";
    public final static String HIST_STROM_EINSPEISUNG = "HIST_STROM_EINSPEISUNG";
    public final static String HIST_STROM_PRODUCTION = "HIST_STROM_PRODUCTION";
    public final static String HIST_STROM_CONSUMPTION = "HIST_STROM_CONSUMPTION";
    public final static String HIST_STROM_WALLBOX = "HIST_STROM_WALLBOX";
    public final static String HIST_GAS = "HIST_GAS";
    public final static String HIST_HEATPUMP_BASEMENT_CONSUMPTION = "HIST_HEATPUMP_BASEMENT_CONSUMPTION";
    public final static String HIST_HEATPUMP_BASEMENT_PRODUCTION = "HIST_HEATPUMP_BASEMENT_PRODUCTION";

    @PostConstruct
    public void postConstruct() {

        elements = new HashMap<>();

        elements
            .put(HIST_THERMOMETER_KINDERZIMMER_1_TEMP, new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER_1, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, new BigDecimal(1)));

        elements.put(HIST_THERMOMETER_KINDERZIMMER_1_HUM, new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER_1, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, new BigDecimal(2)));

        elements.put(HIST_THERMOMETER_KINDERZIMMER_2_TEMP, new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER_2, Datapoint.ACTUAL_TEMPERATURE),
                        HistoryStrategy.AVG, new BigDecimal(1)));

        elements.put(HIST_THERMOMETER_KINDERZIMMER_2_HUM, new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER_2, Datapoint.HUMIDITY),
                HistoryStrategy.AVG, new BigDecimal(2)));

        elements.put(HIST_THERMOMETER_SCHLAFZIMMER_1_TEMP, new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, new BigDecimal(1)));

        elements.put(HIST_THERMOMETER_SCHLAFZIMMER_1_HUM, new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, new BigDecimal(2)));

        elements.put(HIST_THERMOMETER_WASCHKUECHE_2_TEMP, new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, new BigDecimal(1)));

        elements.put(HIST_THERMOMETER_WASCHKUECHE_2_HUM, new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, new BigDecimal(2)));

        elements.put(HIST_THERMOMETER_AUSSEN_2_TEMP, new HistoryElement(homematicCommandBuilder.read(Device.AUSSENTEMPERATUR, Datapoint.VALUE),
                HistoryStrategy.AVG, new BigDecimal(1)));

        elements.put(HIST_STROM_BEZUG, new HistoryElement(homematicCommandBuilder.read(Device.STROMZAEHLER_BEZUG, Datapoint.IEC_ENERGY_COUNTER),
            HistoryStrategy.MAX, new BigDecimal("0.1")));

        elements.put(HIST_STROM_EINSPEISUNG, new HistoryElement(homematicCommandBuilder.read(Device.STROMZAEHLER_EINSPEISUNG, Datapoint.IEC_ENERGY_COUNTER),
                HistoryStrategy.MAX, new BigDecimal("0.5")));

        elements.put(HIST_STROM_PRODUCTION, new HistoryElement(new PersistentCacheCommand(de.fimatas.home.library.model.PersistentCacheKey.ELECTRIC_POWER_PRODUCTION_COUNTER_HAUS),
                HistoryStrategy.MAX, new BigDecimal("0.5")));

        elements.put(HIST_STROM_CONSUMPTION, new HistoryElement(new PersistentCacheCommand(de.fimatas.home.library.model.PersistentCacheKey.ELECTRIC_POWER_CONSUMPTION_COUNTER_HAUD),
                HistoryStrategy.MAX, new BigDecimal("0.1")));

        elements.put(HIST_STROM_WALLBOX, new HistoryElement(homematicCommandBuilder.read(Device.STROMZAEHLER_WALLBOX, Datapoint.ENERGY_COUNTER),
            HistoryStrategy.MAX, new BigDecimal(1000)));

        elements.put(HIST_GAS, new HistoryElement(homematicCommandBuilder.read(Device.GASZAEHLER, Datapoint.GAS_ENERGY_COUNTER),
                HistoryStrategy.MAX, new BigDecimal("0.2")));

        elements.put(HIST_HEATPUMP_BASEMENT_CONSUMPTION, new HistoryElement(new PersistentCacheCommand(de.fimatas.home.library.model.PersistentCacheKey.ELECTRIC_POWER_CONSUMPTION_COUNTER_HEATPUMP_KELLER),
                HistoryStrategy.MAX, new BigDecimal("0.5")));

        elements.put(HIST_HEATPUMP_BASEMENT_PRODUCTION, new HistoryElement(new PersistentCacheCommand(de.fimatas.home.library.model.PersistentCacheKey.WARMTH_POWER_PRODUCTION_COUNTER_HEATPUMP_KELLER),
                HistoryStrategy.MAX, new BigDecimal("1.0")));
    }

    public HistoryElement get(String histKey) {
        return elements.get(histKey);
    }

    public List<HistoryElement> list() {
        return elements.values().stream().toList();
    }
}
