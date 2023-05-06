package de.fimatas.home.library.domain.model;

import de.fimatas.home.library.homematic.model.Device;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class PowerMeter extends AbstractDeviceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public PowerMeter() {
        super();
    }

    private ValueWithTendency<BigDecimal> actualConsumption;
}
