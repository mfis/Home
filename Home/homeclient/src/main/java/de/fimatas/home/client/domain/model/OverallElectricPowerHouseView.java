package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OverallElectricPowerHouseView extends View {

    private String gridActualDirectionIcon = "";

    private View consumption = new PowerView();

    private View gridActualDirection = new PowerView();

    private View pv = new PowerView();

    private View gridPurchase = new PowerView();

    private View gridFeed = new PowerView();
}
