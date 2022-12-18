package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class WidgetGroupView extends View {

    private Map<String, View> captionAndValue = new LinkedHashMap<>();
}
