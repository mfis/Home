package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class HeatpumpRoofView extends View {

    private String label = "";

    private String placeSubtitle = "";

    private String linkCoolAuto = "#";

    private String linkCoolMin = "#";

    private String linkHeatAuto = "#";

    private String linkHeatMin = "#";

    private String linkFanAuto = "#";

    private String linkFanMin = "#";

    private String linkDryTimer = "#";

    private String linkHeatTimer1 = "#";

    private String linkHeatTimer2 = "#";

    private String linkHeatTimer3 = "#";

    private String linkCoolTimer1 = "#";

    private String linkCoolTimer2 = "#";

    private String linkCoolTimer3 = "#";

    private String linkOff = "#";

    private String linkRefresh = "#";

    private boolean stateUnknown = false;

    private List<ValueWithCaption> otherPlaces = new LinkedList<>();

}
