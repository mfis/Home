package de.fimatas.home.controller.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.PostConstruct;
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
            .add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, 1));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_KINDERZIMMER, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, 2));

        elements
            .add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, 1));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_SCHLAFZIMMER, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, 2));

        elements
            .add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.ACTUAL_TEMPERATURE),
                HistoryStrategy.AVG, 1));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.THERMOMETER_WASCHKUECHE, Datapoint.HUMIDITY),
            HistoryStrategy.AVG, 2));

        elements.add(
            new HistoryElement(homematicCommandBuilder.read(Device.AUSSENTEMPERATUR, Datapoint.VALUE), HistoryStrategy.AVG, 1));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.STROMZAEHLER_GESAMT, Datapoint.ENERGY_COUNTER),
            HistoryStrategy.MAX, 1000));

        elements.add(new HistoryElement(homematicCommandBuilder.read(Device.STROMZAEHLER_WALLBOX, Datapoint.ENERGY_COUNTER),
            HistoryStrategy.MAX, 1000));
    }

    public List<HistoryElement> list() {
        return Collections.unmodifiableList(elements);
    }
}
