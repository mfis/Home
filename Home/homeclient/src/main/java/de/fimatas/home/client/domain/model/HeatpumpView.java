package de.fimatas.home.client.domain.model;

import lombok.Data;

@Data
public class HeatpumpView extends View {

    private String label = "";

    private String linkCoolAuto = "#";

    private String linkCoolMin = "#";

    private String linkHeatAuto = "#";

    private String linkHeatMin = "#";

    private String linkFanAuto = "#";

    private String linkFanMin = "#";

    private String linkTimer = "#";

    private String linkOff = "#";

}
