package de.fimatas.home.client.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedList;
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class OverallElectricPowerHouseView extends View {

    private PowerView consumption = new PowerView();

    private PowerView gridActualDirection = new PowerView();

    private PowerView pv = new PowerView();

    private PowerView gridPurchase = new PowerView();

    private PowerView gridFeed = new PowerView();

    private String timestampStatePV = "unbekannt";

    private String timestampStateGrid = "unbekannt";

    private String pvSelfConsumptionPercentage = null;

    private String pvSelfConsumptionPercentageHistoryKey = null;

    private String batteryStateOfCharge;

    private String batteryCapacity;

    private String batteryState;

    private String batteryColorClass;

    private String batteryIcon;

    private String batteryDirectionArrowClass = "#";

    private List<ValueWithCaption> pvDetails = new LinkedList<>();
}
