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
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static de.fimatas.home.controller.domain.service.HouseService.AUTOMATIC;

@Component
@CommonsLog
public class ElectricVehicleService {

    @Autowired
    private UploadService uploadService;

    @Autowired
    private LiveActivityService liveActivityService;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    @Autowired
    private EvChargingDAO evChargingDAO;

    @Autowired
    private HomematicAPI homematicAPI;

    @Autowired
    private HouseService houseService;

    @Autowired
    private PushService pushService;

    @Autowired
    private HomematicCommandBuilder homematicCommandBuilder;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @Autowired
    private Environment env;

    private final String STATEHANDLER_GROUPNAME_BATTERY = "ev-battery";

    private final String STATEHANDLER_GROUPNAME_CHARGELIMIT = "ev-limit";

    private final String STATEHANDLER_GROUPNAME_SELECTED_EV = "ev-selected";

    private final String STATEHANDLER_GROUPNAME_CHARGING_USER = "ev-user";

    private final Device COUNTER_DEVICE = Device.STROMZAEHLER_WALLBOX;

    private final Device WALLBOX_SWITCH_DEVICE = Device.SCHALTER_WALLBOX;

    @SuppressWarnings("FieldCanBeLocal") private final short CHARGING_LIMIT_MAX_DIFF = 7;

    private final BigDecimal HUNDRET = new BigDecimal(100);

    private final BigDecimal THOUSAND = new BigDecimal(1000);

    private final BigDecimal MINUTES_A_HOUR = new BigDecimal(60);

    @SuppressWarnings("FieldCanBeLocal")
    private final int MAX_WATTAGE_SMALL_CONSUMER = 2000;

    private boolean firstRun = true;

    private final List<String> sentChargingProblemPushTimestampCache = new ArrayList<>();

    @Scheduled(cron = "10 59 * * * *")
    public void refreshModel() {

        if(evChargingDAO.isSetupIsRunning()){
            return;
        }

        final List<State> states = stateHandlerDAO.readStates(STATEHANDLER_GROUPNAME_BATTERY);

        var newModel = new ElectricVehicleModel();
        states.forEach(s-> {
            var state = new ElectricVehicleState(ElectricVehicle.valueOf(s.getStatename()), Short.parseShort(s.getValue()), s.getTimestamp());
            readChargeLimit(state);
            readAdditionalChargingPercentage(state);
            calculateEstimatedChargingTime(state);
            newModel.getEvMap().put(ElectricVehicle.valueOf(s.getStatename()), state);
        });

        // add new ev's
        Arrays.stream(ElectricVehicle.values()).filter(ev -> !newModel.getEvMap().containsKey(ev)).forEach(ev -> {
            var newEvState = new ElectricVehicleState(ev, (short) 0, uniqueTimestampService.get());
            readChargeLimit(newEvState);
            newModel.getEvMap().put(ev, newEvState);
        });

        // wallbox-connected ev
        ElectricVehicle connected = readConnectedEv();
        if(connected!=null){
            newModel.getEvMap().get(connected).setConnectedToWallbox(true);
        }

        if(log.isDebugEnabled()){
            var actual = newModel.getEvMap().get(connected).getBatteryPercentage() + newModel.getEvMap().get(connected).getAdditionalChargingPercentage();
            var limit = newModel.getEvMap().get(connected).getChargeLimit() == null ? 100 : newModel.getEvMap().get(connected).getChargeLimit().getPercentage();
            log.debug("refreshModel() actual=" + actual + " limit=" + limit);
        }

        ModelObjectDAO.getInstance().write(newModel);
        uploadService.uploadToClient(newModel);
        liveActivityService.newModel(ElectricVehicleModel.class);
    }

    private void calculateEstimatedChargingTime(ElectricVehicleState state) {

        state.getChargingTime().clear();

        if(state.getChargeLimit()==null || state.getChargingCapacity()==null){
            return;
        }
        final int batteryPercentage = state.getBatteryPercentage() + state.getAdditionalChargingPercentage();

        final int toChargePercentage = state.getChargeLimit().getPercentage() - batteryPercentage;
        final BigDecimal toChargeWattMinutes = state.getChargingCapacity()
                .divide(HUNDRET, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(toChargePercentage))
                .multiply(THOUSAND).multiply(MINUTES_A_HOUR);

        final short voltage = Short.parseShort(Objects.requireNonNull(env.getProperty("grid.voltage")));
        final short phaseCount = Short.parseShort(Objects.requireNonNull(env.getProperty("ev." + state.getElectricVehicle().name() + ".chargingPhaseCount")));
        final String[] amperages =
                StringUtils.split(Objects.requireNonNull(env.getProperty("ev." + state.getElectricVehicle().name() + ".chargingLevelAmpere")), ',');

        Arrays.stream(amperages).forEach(a -> {
            final short amperage = Short.parseShort(a);
            final int wattage = voltage * phaseCount * amperage;
            BigDecimal minutesToCharge = BigDecimal.ZERO;
            if(state.getChargeLimit().getPercentage() > batteryPercentage){
                minutesToCharge = toChargeWattMinutes.divide(new BigDecimal(wattage), 1, RoundingMode.HALF_UP);
            }
            var chargingTime = new EvChargingTime();
            chargingTime.setVoltage(voltage);
            chargingTime.setPhaseCount(phaseCount);
            chargingTime.setAmperage(amperage);
            chargingTime.setMinutes(minutesToCharge.intValue());
            state.getChargingTime().add(chargingTime);
        });
    }

    private void readChargeLimit(ElectricVehicleState state) {
        final State readState = stateHandlerDAO.readState(STATEHANDLER_GROUPNAME_CHARGELIMIT, state.getElectricVehicle().name());
        if(readState!=null){
            state.setChargeLimit(ChargeLimit.valueOf(readState.getValue()));
        }
    }

    public void updateBatteryPercentage(ElectricVehicle electricVehicle, String percentageString){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_BATTERY, electricVehicle.name(), Short.toString(Short.parseShort(percentageString)));
        startNewChargingEntryAndRefreshModel();
    }

    public void saveChargingUser(String user){
        CompletableFuture.runAsync(() -> stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_CHARGING_USER, STATEHANDLER_GROUPNAME_CHARGING_USER, user));
    }

    public void updateSelectedEvForWallbox(ElectricVehicle electricVehicle){
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_SELECTED_EV, STATEHANDLER_GROUPNAME_SELECTED_EV, electricVehicle.name());
        startNewChargingEntryAndRefreshModel();
    }

    public void startNewChargingEntryAndRefreshModel(){
        if(evChargingDAO.activeChargingOnDB()){
            log.debug("startNewChargingEntryAndRefreshModel() -> finishAllChargingEntries()");
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

        final BigDecimal chargingCapacity = calculateChargingCapacity(state.getElectricVehicle(), false);
        state.setChargingCapacity(chargingCapacity);

        final List<EvChargeDatabaseEntry> entries = evChargingDAO.read(state.getElectricVehicle(), state.getBatteryPercentageTimestamp());
        if(entries.isEmpty()){
            return;
        }
        final BigDecimal sum = entries.stream().map(EvChargeDatabaseEntry::countValueAsKWH).reduce(BigDecimal.ZERO, BigDecimal::add);

        if(sum.compareTo(BigDecimal.ZERO) > 0){
            final BigDecimal addPercentage = sum
                    .divide(chargingCapacity, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100.0"));
            state.setAdditionalChargingPercentage(addPercentage.shortValue());
        }
        state.setActiveCharging(entries.stream().anyMatch(e -> !e.finished()));
        state.setChargingTimestamp(entries.stream().map(EvChargeDatabaseEntry::getChangeTS).max(LocalDateTime::compareTo).orElse(null));
    }

    @Scheduled(cron = "0 00 00 * * *")
    public void resetCache() {
        sentChargingProblemPushTimestampCache.clear();
    }

    @Scheduled(cron = "5 * * * * *")
    public void scheduledCheckChargingState() {
        if(firstRun){
            try {
                refreshModel();
                Arrays.stream(ElectricVehicle.values()).filter(ev -> !ev.isOther()).forEach(ev -> calculateChargingCapacity(ev, true));
            } catch (Exception e) {
                log.error("Could not initialize ElectricVehicleService completly.", e);
            } finally {
                firstRun = false;
            }
        }
        checkChargingState();
    }

    private synchronized boolean checkChargingState() {

        if(isDeviceConnectionProblem() || ModelObjectDAO.getInstance().readElectricVehicleModel() == null){
            return false;
        }

        if(isWallboxSwitchOff() && !evChargingDAO.activeChargingOnDB()){
            return false;
        }

        final ElectricVehicleState connectedElectricVehicleState =
                ModelObjectDAO.getInstance().readElectricVehicleModel().getEvMap().values().stream()
                        .filter(ElectricVehicleState::isConnectedToWallbox).findFirst().orElse(null);
        if(connectedElectricVehicleState==null){
            return false;
        }

        // update
        evChargingDAO.write(connectedElectricVehicleState.getElectricVehicle(), readEnergyCounterValue(), EvChargePoint.WALLBOX1);
        checkForChargingProblem(connectedElectricVehicleState.getElectricVehicle());

        var chargingFinishedState = isChargingFinished(connectedElectricVehicleState);
        if(chargingFinishedState.isSwitchWallboxOff()){
            switchWallboxOff();
            sentPushNotification(chargingFinishedState);
        }

        if(isWallboxSwitchOff()){
            log.debug("scheduledCheckChargingState() -> isWallboxSwitchOff() -> finishAllChargingEntries()");
            finishAllChargingEntries();  // wallbox off and last counter written -> finish
        }

        refreshModel();
        return true;
    }

    private void checkForChargingProblem(ElectricVehicle electricVehicle) {
        try {
            if(electricVehicle == ElectricVehicle.SMALL && !isWallboxSwitchOff()){
                var wallboxPowerMeter = ModelObjectDAO.getInstance().readHouseModel().getWallboxElectricalPowerConsumption();
                if(!wallboxPowerMeter.isUnreach() && wallboxPowerMeter.getActualConsumption().getValue().intValue() > MAX_WATTAGE_SMALL_CONSUMER){
                    CompletableFuture.runAsync(() ->
                            pushService.sendErrorMessage("Zu hohe Stromabnahme von Kleinverbraucher an Wallbox!"));
                    switchWallboxOff();
                }
                return;
            }
            final EvChargeDatabaseEntry activeCharging = evChargingDAO.readActiveCharging(electricVehicle);
            String cacheKey = activeCharging.getStartTS().toString();
            if(!sentChargingProblemPushTimestampCache.contains(cacheKey)
                    && Math.abs(ChronoUnit.MINUTES.between(LocalDateTime.now(), activeCharging.getStartTS())) >= 8
                    && activeCharging.countValueAsKWH().compareTo(BigDecimal.ZERO) == 0){
                CompletableFuture.runAsync(() ->
                        pushService.sendErrorMessage("Ladevorgang " + electricVehicle.getCaption() + " konnte nicht gestartet werden."));
                sentChargingProblemPushTimestampCache.add(cacheKey);
            }
        }catch (Exception e){
            log.error("checkForChargingProblem()", e);
        }
    }

    private void sentPushNotification(ChargingFinishedState chargingFinishedState) {
        var state = stateHandlerDAO.readState(STATEHANDLER_GROUPNAME_CHARGING_USER, STATEHANDLER_GROUPNAME_CHARGING_USER);
        if(state != null){
            pushService.chargeFinished(chargingFinishedState == ChargingFinishedState.FINISHED_EARLY, state.getValue());
        }
    }

    private boolean isDeviceConnectionProblem() {
        return homematicAPI.isDeviceUnreachableOrNotSending(COUNTER_DEVICE)
                || homematicAPI.isDeviceUnreachableOrNotSending(WALLBOX_SWITCH_DEVICE);
    }

    private BigDecimal readEnergyCounterValue() {
        return homematicAPI.getAsBigDecimal(homematicCommandBuilder.read(COUNTER_DEVICE, Datapoint.ENERGY_COUNTER));
    }

    private ChargingFinishedState isChargingFinished(ElectricVehicleState connectedElectricVehicleState){

        if(connectedElectricVehicleState.getElectricVehicle() == ElectricVehicle.SMALL) {
            return ChargingFinishedState.NO_CHARGING_ONLY_CONSUMING;
        }

        readAdditionalChargingPercentage(connectedElectricVehicleState);
        var actual = connectedElectricVehicleState.getBatteryPercentage() + connectedElectricVehicleState.getAdditionalChargingPercentage();
        var limit = connectedElectricVehicleState.getChargeLimit() == null ? 100 : connectedElectricVehicleState.getChargeLimit().getPercentage();

        log.debug("isChargingFinished() actual=" + actual + " limit=" + limit);

        if(actual >= limit && limit < 100){
            log.debug("isChargingFinished() return true -> limit");
            return ChargingFinishedState.FINISHED_NORMAL;
        }

        final LocalDateTime maxChangeTimestamp = evChargingDAO.maxChangeTimestamp();
        if(maxChangeTimestamp!=null &&
                ChronoUnit.SECONDS.between(maxChangeTimestamp, uniqueTimestampService.get()) > minSecondsNoChangeUntilSwitchOffWallbox()){
            log.debug("isChargingFinished() return true -> no charge");
            return (limit - actual > CHARGING_LIMIT_MAX_DIFF)?ChargingFinishedState.FINISHED_EARLY:ChargingFinishedState.FINISHED_NORMAL;
        }
        return ChargingFinishedState.STILL_CHARGING;
    }

    private void switchWallboxOff() {
        log.debug("switchWallboxOff()");
        houseService.togglestate(WALLBOX_SWITCH_DEVICE, false);
        if(isWallboxSwitchAutomatic()){
            homematicAPI.executeCommand(homematicCommandBuilder.write(WALLBOX_SWITCH_DEVICE, AUTOMATIC, false));
        }
        houseService.refreshHouseModel(false);
    }

    private boolean isWallboxSwitchOff(){
        return !homematicAPI.getAsBoolean(homematicCommandBuilder.read(WALLBOX_SWITCH_DEVICE, Datapoint.STATE));
    }

    private boolean isWallboxSwitchAutomatic(){
        return homematicAPI.getAsBoolean(homematicCommandBuilder.read(WALLBOX_SWITCH_DEVICE, AUTOMATIC));
    }

    private long minSecondsNoChangeUntilSwitchOffWallbox(){
        return Math.max(HomeAppConstants.MODEL_DEFAULT_INTERVAL_SECONDS, HomeAppConstants.CHARGING_STATE_CHECK_INTERVAL_SECONDS) * 15;
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

    public void updateChargeLimit(ElectricVehicle electricVehicle, String value) {
        stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_CHARGELIMIT, electricVehicle.name(), ChargeLimit.valueOf(value).name());
        startNewChargingEntryAndRefreshModel();
    }

    @Getter
    private enum ChargingFinishedState{
        FINISHED_NORMAL(true),
        FINISHED_EARLY(true),
        STILL_CHARGING(false),
        NO_CHARGING_ONLY_CONSUMING(false),
        ;
        private final boolean switchWallboxOff;
        ChargingFinishedState(boolean switchWallboxOff){
            this.switchWallboxOff = switchWallboxOff;
        }
    }
}
