package de.fimatas.home.controller.service;

import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.dao.EvChargingDAO;
import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@CommonsLog
public class ElectricVehicleService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    @Autowired
    private EvChargingDAO evChargingDAO;

    @Autowired
    private HomematicAPI homematicAPI;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    private final String STATEHANDLER_GROUPNAME_BATTERY = "ev-battery";

    private final String STATEHANDLER_GROUPNAME_SELECTED_EV = "ev-selected";

    private final int CHARGEPOINT_FIX = 1;

    private final Device COUNTER_DEVICE = Device.STROMZAEHLER_GESAMT; // FIXME

    private final Device WALLBOX_SWITCH_DEVICE = Device.SCHALTER_WALLBOX;

    private ChargingState cachedChargingState = ChargingState.UNKNOWN;

    private ElectricVehicle cachedConnectedEv = null;

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                if(evChargingDAO.unfinishedChargingOnDB()){
                    cachedChargingState = ChargingState.ACTIVE;
                }else{
                    cachedChargingState = ChargingState.FINISHED;
                }
                final List<State> connectedEntry = stateHandlerDAO.readStates(STATEHANDLER_GROUPNAME_SELECTED_EV);
                if(!connectedEntry.isEmpty()){
                    cachedConnectedEv = ElectricVehicle.valueOf(connectedEntry.get(0).getValue());
                }
                refreshModel();
            } catch (Exception e) {
                log.error("Could not initialize ElectricVehicleService completly.", e);
            }
        });
    }

    public void refreshModel() {

        final List<State> states = stateHandlerDAO.readStates(STATEHANDLER_GROUPNAME_BATTERY);

        var newModel = new ElectricVehicleModel();
        states.forEach(s-> {
            var state = new ElectricVehicleState(ElectricVehicle.valueOf(s.getStatename()), Short.parseShort(s.getValue()), s.getTimestamp());
            final List<EvChargeDatabaseEntry> entries = evChargingDAO.read(state.getElectricVehicle(), state.getTimestamp());
            final BigDecimal sum = entries.stream().map(e -> e.countValueAsKWH()).reduce(BigDecimal.ZERO, BigDecimal::add);
            if(sum.compareTo(BigDecimal.ZERO) > 0){
                final BigDecimal addPercentage = sum.divide(new BigDecimal(36.5), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100.0));
                log.info("ADDITIONAL:" + sum + " -> " + addPercentage + "%");
                state.setAdditionalChargingPercentage(addPercentage.shortValue());
            }
            newModel.getEvMap().put(ElectricVehicle.valueOf(s.getStatename()), state);
        });

        Arrays.stream(ElectricVehicle.values()).filter(ev -> !newModel.getEvMap().containsKey(ev)).forEach(ev ->
                newModel.getEvMap().put(ev, new ElectricVehicleState(ev, (short) 0, uniqueTimestampService.get())));

        // wallbox-connected ev
        if(cachedConnectedEv!=null){
            newModel.getEvMap().get(cachedConnectedEv).setConnectedToWallbox(true);
        }

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }

    public void updateBatteryPercentage(ElectricVehicle electricVehicle, String percentageString){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_BATTERY, electricVehicle.name(), Short.toString(Short.parseShort(percentageString)));
        refreshModel();
    }

    public void updateSelectedEvForWallbox(ElectricVehicle electricVehicle){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_SELECTED_EV, STATEHANDLER_GROUPNAME_SELECTED_EV, electricVehicle.name());
        cachedConnectedEv = electricVehicle;
        refreshModel();
    }

    // FIXME: wenn beim setzen bereits eine ladung aufgezeichnet wird, diese beenden und neue starten
    // startts muss dann immer gleich oder nach setz-datum prozet liegen
    // FIXME: finnished erkennen über schalter im houseservice??
    // FIXME: direkt nach einschalten counter abfragen um startwert richtig zu erfassen. trigger?

    @Scheduled(initialDelay = 1000 * 20, fixedDelay = (1000 * HomeAppConstants.CHARGING_STATE_CHECK_INTERVAL_SECONDS) + 234)
    private void scheduledCheckChargingState() {

        if(homematicAPI.isDeviceUnreachableOrNotSending(COUNTER_DEVICE)
                || homematicAPI.isDeviceUnreachableOrNotSending(WALLBOX_SWITCH_DEVICE)){
            return; // unreachable -> end
        }

        if(!homematicAPI.getAsBoolean(homematicCommandBuilder.read(WALLBOX_SWITCH_DEVICE, Datapoint.STATE))){
            if(cachedChargingState == ChargingState.FINISHED){
                return; // wallbox off and all known chargings finished -> end
            }
        }

        // update
        final BigDecimal val = homematicAPI.getAsBigDecimal(homematicCommandBuilder.read(COUNTER_DEVICE, Datapoint.ENERGY_COUNTER));
        evChargingDAO.write(ElectricVehicle.EUP, val, EvChargePoint.WALLBOX1);
        cachedChargingState = ChargingState.ACTIVE;

        if(!homematicAPI.getAsBoolean(homematicCommandBuilder.read(WALLBOX_SWITCH_DEVICE, Datapoint.STATE))){
            evChargingDAO.finishAll(); // wallbox off and last counter written -> finish
            cachedChargingState = ChargingState.FINISHED;
        }

        refreshModel();
    }

    private enum ChargingState{
        ACTIVE, FINISHED, UNKNOWN;
    }
}
