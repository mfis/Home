package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ElectroVehicleView extends View {

    private String caption = "";

    private String link = "#";
}
