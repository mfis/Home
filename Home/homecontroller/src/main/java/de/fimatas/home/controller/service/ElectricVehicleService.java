package de.fimatas.home.controller.service;

import de.fimatas.home.controller.api.HomematicAPI;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.dao.EvChargingDAO;
import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.util.HomeAppConstants;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
    private HouseService houseService;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @Autowired
    private Environment env;

    private final String STATEHANDLER_GROUPNAME_BATTERY = "ev-battery";

    private final String STATEHANDLER_GROUPNAME_SELECTED_EV = "ev-selected";

    private final Device COUNTER_DEVICE = Device.STROMZAEHLER_WALLBOX; // FIXME

    private final Device WALLBOX_SWITCH_DEVICE = Device.SCHALTER_WALLBOX;

    @PostConstruct
    private void init() {
        CompletableFuture.runAsync(() -> {
            try {
                refreshModel();
                Arrays.stream(ElectricVehicle.values()).filter(ev -> !ev.isOther()).forEach(ev -> calculateChargingCapacity(ev, true));
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
            readAdditionalChargingPercentage(state);
            newModel.getEvMap().put(ElectricVehicle.valueOf(s.getStatename()), state);
        });

        // add new ev's
        Arrays.stream(ElectricVehicle.values()).filter(ev -> !newModel.getEvMap().containsKey(ev)).forEach(ev ->
                newModel.getEvMap().put(ev, new ElectricVehicleState(ev, (short) 0, uniqueTimestampService.get())));

        // wallbox-connected ev
        ElectricVehicle connected = readConnectedEv();
        if(connected!=null){
            newModel.getEvMap().get(connected).setConnectedToWallbox(true);
        }

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
    }


    public void updateBatteryPercentage(ElectricVehicle electricVehicle, String percentageString){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_BATTERY, electricVehicle.name(), Short.toString(Short.parseShort(percentageString)));
        startNewChargingEntryAndRefreshModel();
    }

    public void updateSelectedEvForWallbox(ElectricVehicle electricVehicle){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_SELECTED_EV, STATEHANDLER_GROUPNAME_SELECTED_EV, electricVehicle.name());
        startNewChargingEntryAndRefreshModel();
    }


    public void startNewChargingEntryAndRefreshModel(){
        if(evChargingDAO.activeChargingOnDB()){
            finishAllChargingEntries();
        }
        if(!checkChargingState()){
            refreshModel();
        }
    }

    private void readAdditionalChargingPercentage(ElectricVehicleState state) {

        if(state.getElectricVehicle().isOther()){
            return;
        }

        final List<EvChargeDatabaseEntry> entries = evChargingDAO.read(state.getElectricVehicle(), state.getBatteryPercentageTimestamp());
        if(entries.isEmpty()){
            return;
        }
        final BigDecimal sum = entries.stream().map(EvChargeDatabaseEntry::countValueAsKWH).reduce(BigDecimal.ZERO, BigDecimal::add);
        if(sum.compareTo(BigDecimal.ZERO) > 0){
            final BigDecimal addPercentage = sum
                    .divide(calculateChargingCapacity(state.getElectricVehicle(), false), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100.0"));
            // log.info("ADDITIONAL:" + sum + " -> " + addPercentage + "%");
            state.setAdditionalChargingPercentage(addPercentage.shortValue());
        }
        state.setActiveCharging(entries.stream().anyMatch(e -> !e.finished()));
        state.setChargingTimestamp(entries.stream().map(EvChargeDatabaseEntry::getChangeTS).max(LocalDateTime::compareTo).orElse(null));
    }

    @Scheduled(initialDelay = 1000 * 20, fixedDelay = (1000 * HomeAppConstants.CHARGING_STATE_CHECK_INTERVAL_SECONDS) + 234)
    private void scheduledCheckChargingState() {
        checkChargingState();
    }

    private synchronized boolean checkChargingState() {

        if(isDeviceConnectionProblem()){
            return false;
        }

        if(isWallboxSwitchOff() && !evChargingDAO.activeChargingOnDB()){
            return false;
        }

        // update
        evChargingDAO.write(readConnectedEv(), readEnergyCounterValue(), EvChargePoint.WALLBOX1);

        if(isNoChargineEnergyCounted()){
            switchWallboxOff();
        }

        if(isWallboxSwitchOff()){
            finishAllChargingEntries();  // wallbox off and last counter written -> finish
        }

        refreshModel();
        return true;
    }

    private boolean isDeviceConnectionProblem() {
        return homematicAPI.isDeviceUnreachableOrNotSending(COUNTER_DEVICE)
                || homematicAPI.isDeviceUnreachableOrNotSending(WALLBOX_SWITCH_DEVICE);
    }

    private BigDecimal readEnergyCounterValue() {
        return homematicAPI.getAsBigDecimal(homematicCommandBuilder.read(COUNTER_DEVICE, Datapoint.ENERGY_COUNTER));
    }

    private boolean isNoChargineEnergyCounted(){
        final LocalDateTime maxChangeTimestamp = evChargingDAO.maxChangeTimestamp();
        return maxChangeTimestamp!=null &&
                ChronoUnit.SECONDS.between(maxChangeTimestamp, uniqueTimestampService.get()) > minSecondsNoChangeUntilSwitchOffWallbox();
    }

    private void switchWallboxOff() {
        houseService.togglestate(WALLBOX_SWITCH_DEVICE, false);
        houseService.refreshHouseModel();
    }

    private boolean isWallboxSwitchOff(){
        return !homematicAPI.getAsBoolean(homematicCommandBuilder.read(WALLBOX_SWITCH_DEVICE, Datapoint.STATE));
    }

    private long minSecondsNoChangeUntilSwitchOffWallbox(){
        return Math.max(HomeAppConstants.MODEL_DEFAULT_INTERVAL_SECONDS, HomeAppConstants.CHARGING_STATE_CHECK_INTERVAL_SECONDS) * 5; // FIXME
    }

    private void finishAllChargingEntries() {
        evChargingDAO.finishAll();
    }

    private ElectricVehicle readConnectedEv(){
        final List<State> connectedEntry = stateHandlerDAO.readStates(STATEHANDLER_GROUPNAME_SELECTED_EV);
        if(!connectedEntry.isEmpty()){
            return ElectricVehicle.valueOf(connectedEntry.get(0).getValue());
        }
        return null;
    }

    private BigDecimal calculateChargingCapacity(ElectricVehicle ev, boolean logging){

        var netCapacityKWh = new BigDecimal(Objects.requireNonNull(env.getProperty("ev." + ev.name() + ".netCapacityKWh")));
        var chargingLossPercentage = new BigDecimal(Objects.requireNonNull(env.getProperty("ev." + ev.name() + ".chargingLossPercentage")));
        var dailyBatteryDegradationPercentage = new BigDecimal(Objects.requireNonNull(env.getProperty("ev." + ev.name() + ".dailyBatteryDegradationPercentage")));
        var dateOfFullCapacity = LocalDate.parse(Objects.requireNonNull(env.getProperty("ev." + ev.name() + ".dateOfFullCapacity"))); // YYYY-MM-DD

        var actualBatteryDegaradationPercentage =
                dailyBatteryDegradationPercentage.multiply(new BigDecimal(ChronoUnit.DAYS.between(dateOfFullCapacity, LocalDate.now())));

        var netCapacityInclusiveDegradation = netCapacityKWh.subtract(netCapacityKWh
                .multiply(actualBatteryDegaradationPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));

        final var chargingCapacity =
                netCapacityInclusiveDegradation.add(netCapacityInclusiveDegradation
                        .multiply(chargingLossPercentage)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));

        if(logging){
            final String text = MessageFormat.format("Batteriezustand {0}: Degradation={1} Nettokapazität={2} Ladekapazität={3}",
                    ev.getCaption(), actualBatteryDegaradationPercentage, netCapacityInclusiveDegradation, chargingCapacity);
            log.info(text);
        }

        return chargingCapacity;
    }
}
