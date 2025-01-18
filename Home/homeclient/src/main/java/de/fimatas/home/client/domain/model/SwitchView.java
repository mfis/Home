package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SwitchView extends View {

    private String label = "";

    private String link = "#";

    private String linkOn = "#";

    private String linkOff = "#";

    private String linkAuto = "#";

    private String linkManual = "#";

    private String linkMinPvBattery = "#";

    private String autoInfoText = "";

    private String buttonCaptionAuto = "";

    private String buttonCaptionManual = "";

    private String showOverflowRange = "";

    private String overflowConsumptionValue = "";

    private String overflowMaxGridValue = "";

    private String overflowMaxGridValueLink = "";

    private String overflowDelayInfo = "";

    private String overflowCounterInfo = "";

    private String overflowPriority = "";

    private List<ValueWithCaption> minPvBatteryPercentages = new LinkedList<>();

}
