package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OverallElectricPowerHouseView extends View {

    private PowerView consumption = new PowerView();

    private PowerView gridActualDirection = new PowerView();

    private PowerView pv = new PowerView();

    private PowerView gridPurchase = new PowerView();

    private PowerView gridFeed = new PowerView();
}
