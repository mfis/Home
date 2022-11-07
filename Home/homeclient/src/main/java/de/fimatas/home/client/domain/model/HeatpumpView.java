package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class HeatpumpView extends View {

    private String label = "";

    private String placeSubtitle = "";

    private String linkCoolAuto = "#";

    private String linkCoolMin = "#";

    private String linkHeatAuto = "#";

    private String linkHeatMin = "#";

    private String linkFanAuto = "#";

    private String linkFanMin = "#";

    private String linkTimer = "#";

    private String linkOff = "#";

    private String linkRefresh = "#";

    private List<ValueWithCaption> otherPlaces = new LinkedList<>();

}
