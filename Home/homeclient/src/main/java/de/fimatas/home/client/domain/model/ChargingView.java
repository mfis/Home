package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class ChargingView extends View {

    private String label = "";

    private String placeSubtitle = "";

    private String linkUpdate = "#";

    private String numericValue = "";

    private String stateActualFlag = "";

    private String stateShortLabel = "";

    private String chargeLimitLink = "";

    private List<ValueWithCaption> chargeLimits = new LinkedList<>();

}
