package de.fimatas.home.library.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class HeatpumpRoof implements Serializable {

    private static final long serialVersionUID = 1L;

    private Place place;

    private HeatpumpRoofPreset heatpumpRoofPreset;

    private Long timer;
}
