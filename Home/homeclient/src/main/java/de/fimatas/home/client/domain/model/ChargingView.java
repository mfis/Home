package de.fimatas.home.client.domain.model;

import lombok.Data;

@Data
public class ChargingView extends View {

    private String label = "";

    private String placeSubtitle = "";

    private String linkUpdate = "#";

    private String numericValue = "";

    private String stateActualFlag = "";

    private String stateShortLabel = "";

}
