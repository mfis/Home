package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class HeatpumpBasementView extends View {

    private String linkRefresh = "#";

    private String timestamp = "";

    private List<ValueWithCaption> datapoints = new LinkedList<>();

}
