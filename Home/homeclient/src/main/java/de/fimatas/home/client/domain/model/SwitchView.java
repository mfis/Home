package de.fimatas.home.client.domain.model;

import lombok.Data;

@Data
public class SwitchView extends View {

    private String label = "";

    private String link = "#";

    private String linkOn = "#";

    private String linkOff = "#";

    private String linkAuto = "#";

    private String linkManual = "#";

    private String autoInfoText = "";

    private String buttonCaptionAuto = "";

    private String buttonCaptionManual = "";

    private String showOverflowRange = "";

    private String overflowConsumptionValue = "";

    private String overflowMaxGridValue = "";

}
