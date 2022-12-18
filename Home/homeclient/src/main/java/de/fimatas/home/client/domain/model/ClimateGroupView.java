package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ClimateGroupView extends View {

    private Map<String, ClimateView> captionAndValue = new LinkedHashMap<>();
}
