package de.fimatas.home.library.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Switch extends AbstractDeviceModel implements Serializable {

    public Switch() {
        super();
    }

    private boolean state;

    private Boolean automation;

    private String automationInfoText;

    private PowerMeter associatedPowerMeter;

    private boolean pvOverflowConfigured;

    private int maxWattageFromGridInOverflowAutomationMode;

    private int defaultWattage;
}
