package de.fimatas.home.controller.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.homematic.model.HistoryStrategy;

@Component
public class History {

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    private List<HistoryElement> elements;

    @PostConstruct
    public void postConstruct() {

        elements = new LinkedList<>();

        elements
            .add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER_1, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER_1, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, new BigDecimal(2)));

        elements
                .add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER_2, Datapoint.ACTUAL_TEMPERATURE),
                        HistoryStrategy.AVG, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER_2, Datapoint.HUMIDITY),
                HistoryStrategy.AVG, new BigDecimal(2)));

        elements
            .add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, new BigDecimal(2)));

        elements
            .add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, new BigDecimal(2)));

        elements.add(
            new HistoryElement(homematicCommandBuilder.read(Device.AUSSENTEMPERATUR, Datapoint.VALUE), HistoryStrategy.AVG, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.STROMZAEHLER_BEZUG, Datapoint.IEC_ENERGY_COUNTER),
            HistoryStrategy.MAX, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.STROMZAEHLER_EINSPEISUNG, Datapoint.IEC_ENERGY_COUNTER),
                HistoryStrategy.MAX, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.ELECTRIC_POWER_PRODUCTION_COUNTER_HOUSE, Datapoint.VALUE),
                HistoryStrategy.MAX, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.ELECTRIC_POWER_CONSUMPTION_COUNTER_HOUSE, Datapoint.VALUE),
                HistoryStrategy.MAX, new BigDecimal(1)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.STROMZAEHLER_WALLBOX, Datapoint.ENERGY_COUNTER),
            HistoryStrategy.MAX, new BigDecimal(1000)));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.GASZAEHLER, Datapoint.GAS_ENERGY_COUNTER),
                HistoryStrategy.MAX, new BigDecimal("0.2")));
    }

    public List<HistoryElement> list() {
        return Collections.unmodifiableList(elements);
    }
}
