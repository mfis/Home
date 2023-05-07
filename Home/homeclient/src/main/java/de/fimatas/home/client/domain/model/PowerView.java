package de.fimatas.home.client.domain.model;

import de.fimatas.home.library.homematic.model.Device;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PowerView extends View {

    private String tendencyIcon = "";

    private String directionIcon = "";

    private String description = "";

    private ChartEntry todayConsumption;

    private Device device;

}
