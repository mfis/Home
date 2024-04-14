package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.annotation.EnablePhotovoltaicsOverflow;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Device;
import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class PhotovoltaicsOverflowService {

    @Autowired
    private HouseService houseService;

    @Autowired
    private PushService pushService;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @Autowired
    private StateHandlerDAO stateHandlerDAO;

    @Autowired
    private Environment env;

    private final LinkedList<OverflowControlledDevice> overflowControlledDevices = new LinkedList<>();

    private final Map<OverflowControlledDevice, OverflowControlledDeviceState> overflowControlledDeviceStates = new HashMap<>();

    private long lastGridElectricStatusTime = -1;

    private final String STATEHANDLER_GROUPNAME_PV_OVERFLOW = "pv-overflow";

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final Log LOG = LogFactory.getLog(PhotovoltaicsOverflowService.class);

    @PostConstruct
    public void init() {
        overflowControlledDevices.clear();
        overflowControlledDeviceStates.clear();
        lastGridElectricStatusTime = -1;
        for(Field field : HouseModel.class.getDeclaredFields()){
            final var enablePhotovoltaicsOverflow = field.getAnnotation(EnablePhotovoltaicsOverflow.class);
            if(enablePhotovoltaicsOverflow != null){
                final var overflowControlledDevice =
                        new OverflowControlledDevice(field,
                                getProperty(field, "shortName"),
                                Integer.parseInt(getProperty(field, "defaultWattage")),
                                Integer.parseInt(getProperty(field, "switchOnDelay")),
                                Integer.parseInt(getProperty(field, "switchOffDelay")),
                                Integer.parseInt(getProperty(field, "defaultPriority")),
                                Integer.parseInt(getProperty(field, "maxDailyOnSwitching"))
                        );
                overflowControlledDevices.add(overflowControlledDevice);
                var ocds = new OverflowControlledDeviceState();
                ocds.lastKnownWattage = overflowControlledDevice.defaultWattage;
                ocds.controlState = ControlState.STABLE;
                ocds.controlStateTimestamp = LocalDateTime.now();
                overflowControlledDeviceStates.put(overflowControlledDevice, ocds);
            }
        }
        sortDevicesAccordingToPriority();
    }

    @Scheduled(cron = "0 00 00 * * *")
    public void resetCounter() {
        overflowControlledDeviceStates.values().forEach(ocds -> ocds.dailyOnSwitchingCounter = 0);

        final var houseModel = ModelObjectDAO.getInstance().readHouseModel();
        for (OverflowControlledDevice ocd : overflowControlledDevices) {
            final var deviceModel = getDeviceModel(houseModel, ocd);
            if (isAutoModeOn(deviceModel)) {
                houseService.toggleautomation(deviceModel.getDevice(), AutomationState.MANUAL);
                if (isActualDeviceSwitchState(deviceModel)){
                    houseService.togglestate(deviceModel.getDevice(), false);
                }
            }
        }
    }

    public HouseModel readOverflowFields(HouseModel houseModel){
        overflowControlledDeviceStates.keySet().forEach(ocd -> {
            Switch switchModel = (Switch) getDeviceModel(houseModel, ocd);
            switchModel.setPvOverflowConfigured(true);
            switchModel.setDefaultWattage(ocd.defaultWattage);
            switchModel.setMaxWattageFromGridInOverflowAutomationMode(readMaxGridWattage(ocd.shortName));
            switchModel.setPvOverflowPriority(ocd.defaultPriority);
            switchModel.setPvOverflowCounterActual(overflowControlledDeviceStates.get(ocd).dailyOnSwitchingCounter);
            switchModel.setPvOverflowCounterMax(ocd.maxDailyOnSwitching);
            switchModel.setPvOverflowDelayOnMinutes(ocd.switchOnDelay);
            switchModel.setPvOverflowDelayOffMinutes(ocd.switchOffDelay);
        });
        return houseModel;
    }

    public void writeOverflowGridWattage(Device device, int value){
        overflowControlledDeviceStates.keySet().forEach(ocd -> {
            Switch switchModel = (Switch) getDeviceModel(ModelObjectDAO.getInstance().readHouseModel(), ocd);
            if(switchModel.getDevice() == device){
                stateHandlerDAO.writeState(STATEHANDLER_GROUPNAME_PV_OVERFLOW, ocd.shortName, Integer.toString(value));
            }
        });
        houseService.refreshHouseModel(true);
    }

    @Async
    public void houseModelRefreshed() {

        final var houseModel = ModelObjectDAO.getInstance().readHouseModel();
        var hasToRefreshHouseModel = false;

        if(!isActualGridDataAvailable(houseModel)){
            return;
        }

        int wattage = houseModel.getGridElectricalPower().getActualConsumption().getValue().intValue();

        // assume grid-powered - turn something off? starting with lowest priotity
        var reverseIterator = new ReverseListIterator<>(overflowControlledDevices);
        while (reverseIterator.hasNext()) {
            var ocd = reverseIterator.next();
            if(wattage >= 0) {
                final var deviceModel = getDeviceModel(houseModel, ocd);
                final var actualDeviceWattage = getActualDeviceWattage(ocd, deviceModel);
                if (isAutoModeOn(deviceModel) && isActualDeviceSwitchState(deviceModel)) {
                    if (wattage > readMaxGridWattage(ocd.shortName)) {
                        switch(overflowControlledDeviceStates.get(ocd).controlState){
                            case STABLE -> setControlState(ocd, ControlState.PREPARE_TO_OFF);
                            case PREPARE_TO_OFF -> {
                                if(isSwitchOffDelayReached(ocd)){
                                    //LOG.info("switch OFF " + deviceModel.getDevice().name());
                                    houseService.togglestate(deviceModel.getDevice(), false);
                                    setControlState(ocd, ControlState.STABLE);
                                    hasToRefreshHouseModel = true;
                                    wattage -= actualDeviceWattage;
                                    pushService.sendNotice("PV-Überschuss: " + deviceModel.getDevice().getDescription() + " ausgeschaltet.");
                                }
                            }
                            case PREPARE_TO_ON -> {
                                LOG.warn("state confusion (check off)!");
                                setControlState(ocd, ControlState.STABLE);
                            }
                        }
                    } else {
                        setControlState(ocd, ControlState.STABLE);
                    }
                } else {
                    setControlState(ocd, ControlState.STABLE);
                }
            }
        }

        // assume pv overflow - turn something on? starting with highest priority
        for (OverflowControlledDevice ocd : overflowControlledDevices) {
            if (wattage < 0) {
                final var deviceModel = getDeviceModel(houseModel, ocd);
                final var actualDeviceWattage = getActualDeviceWattage(ocd, deviceModel);
                if (isAutoModeOn(deviceModel) && !isActualDeviceSwitchState(deviceModel)) {
                    var minWattsFromPV = (actualDeviceWattage - readMaxGridWattage(ocd.shortName)) * -1;
                    var releaseable = sumOfReleaseablePvWattageWithLowerProprity(ocd, houseModel);
                    if (wattage + releaseable <= minWattsFromPV) {
                        switch (overflowControlledDeviceStates.get(ocd).controlState) {
                            case STABLE -> setControlState(ocd, ControlState.PREPARE_TO_ON);
                            case PREPARE_TO_ON -> {
                                if (isSwitchOnDelayReachedAndAllowed(ocd)) {
                                    //LOG.info("switch ON " + deviceModel.getDevice().name() + " #" +
                                      //      overflowControlledDeviceStates.get(ocd).dailyOnSwitchingCounter + "/" + ocd.maxDailyOnSwitching);
                                    var wattsToRelease = minWattsFromPV - wattage;
                                    var releasedWatts = releasePvWatts(wattsToRelease, ocd, houseModel);
                                    if(releasedWatts < wattsToRelease){ // both values negative!
                                        houseService.togglestate(deviceModel.getDevice(), true);
                                        setControlState(ocd, ControlState.STABLE);
                                        hasToRefreshHouseModel = true;
                                        overflowControlledDeviceStates.get(ocd).dailyOnSwitchingCounter += 1;
                                        wattage += actualDeviceWattage;
                                        pushService.sendNotice("PV-Überschuss: " + deviceModel.getDevice().getDescription() + " eingeschaltet.");
                                    }
                                }
                            }
                            case PREPARE_TO_OFF -> {
                                LOG.warn("state confusion (check on)!");
                                setControlState(ocd, ControlState.STABLE);
                            }
                        }
                    } else {
                        setControlState(ocd, ControlState.STABLE);
                    }
                } else {
                    setControlState(ocd, ControlState.STABLE);
                }
            }
        }

        if(hasToRefreshHouseModel){
            houseService.refreshHouseModel(false);
        }
    }

    private int releasePvWatts(int wattsToRelease, OverflowControlledDevice ocd, HouseModel houseModel) {
        int released = 0;
        var reverseIterator = new ReverseListIterator<>(overflowControlledDevices);
        while (reverseIterator.hasNext()) {
            var ocdToRelease = reverseIterator.next();
            if(ocd.shortName.equals(ocdToRelease.shortName) || released <= wattsToRelease){
                break;
            }else{
                final var deviceModel = getDeviceModel(houseModel, ocdToRelease);
                if (isAutoModeOn(deviceModel) && isActualDeviceSwitchState(deviceModel)) {
                    houseService.togglestate(deviceModel.getDevice(), false);
                    setControlState(ocdToRelease, ControlState.STABLE);
                    pushService.sendNotice("PV-Überschuss: " + deviceModel.getDevice().getDescription() + " für höher priorisiertes Gerät ausgeschaltet.");
                    released -= getActualDeviceWattage(ocdToRelease, deviceModel);
                }
            }
        }
        return released;
    }

    private int sumOfReleaseablePvWattageWithLowerProprity(OverflowControlledDevice ocd, HouseModel houseModel){
        int sum = 0;
        var reverseIterator = new ReverseListIterator<>(overflowControlledDevices);
        while (reverseIterator.hasNext()) {
            var ocdToSum = reverseIterator.next();
            if(ocd.shortName.equals(ocdToSum.shortName)){
                break;
            }else{
                final var deviceModel = getDeviceModel(houseModel, ocdToSum);
                if (isAutoModeOn(deviceModel) && isActualDeviceSwitchState(deviceModel)) {
                    sum -= getActualDeviceWattage(ocdToSum, deviceModel);
                }
            }
        }
        return sum;
    }

    private boolean isActualGridDataAvailable(HouseModel houseModel){
        if(houseModel == null || houseModel.getGridElectricalPower() == null
                || houseModel.getGridElectricalPower().getActualConsumption() == null
                || houseModel.getGridElectricalPower().getActualConsumption().getValue() == null){
            return false;
        }
        long diffGridElectricStatusTime = Math.abs(houseModel.getGridElectricStatusTime() - lastGridElectricStatusTime);
        if(diffGridElectricStatusTime < 5000){
            return false;
        }
        lastGridElectricStatusTime = houseModel.getGridElectricStatusTime();
        return true;
    }

    private boolean isSwitchOnDelayReachedAndAllowed(OverflowControlledDevice ocd) {
        long minutesBetween = ChronoUnit.MINUTES.between(
                uniqueTimestampService.getNonUnique(),
                overflowControlledDeviceStates.get(ocd).controlStateTimestamp);
        return Math.abs(minutesBetween) >= ocd.switchOnDelay
                && overflowControlledDeviceStates.get(ocd).dailyOnSwitchingCounter <= ocd.maxDailyOnSwitching;
    }

    private boolean isSwitchOffDelayReached(OverflowControlledDevice ocd) {
        long between = ChronoUnit.MINUTES.between(
                uniqueTimestampService.getNonUnique(),
                overflowControlledDeviceStates.get(ocd).controlStateTimestamp);
        return Math.abs(between) >= ocd.switchOffDelay;
    }

    private void setControlState(OverflowControlledDevice ocd, ControlState controlState) {
        overflowControlledDeviceStates.get(ocd).controlState = controlState;
        overflowControlledDeviceStates.get(ocd).controlStateTimestamp = uniqueTimestampService.getNonUnique();
    }

    private AbstractDeviceModel getDeviceModel(HouseModel houseModel, OverflowControlledDevice ocd) {
        return houseModel.lookupField(ocd.field, AbstractDeviceModel.class);
    }

    private Integer getActualDeviceWattage(OverflowControlledDevice ocd, AbstractDeviceModel deviceModel) {
        if(deviceModel instanceof Switch switchDevice && switchDevice.getAssociatedPowerMeter() != null
                && switchDevice.getAssociatedPowerMeter().getActualConsumption() != null
                && switchDevice.getAssociatedPowerMeter().getActualConsumption().getValue() != null){
            int wattage = switchDevice.getAssociatedPowerMeter().getActualConsumption().getValue().abs().intValue();
            if(wattage != 0){
                overflowControlledDeviceStates.get(ocd).lastKnownWattage = wattage;
                return wattage;
            }
        }
        return overflowControlledDeviceStates.get(ocd).lastKnownWattage;
    }

    private boolean isActualDeviceSwitchState(AbstractDeviceModel deviceModel) {
        if(deviceModel instanceof Switch switchDevice){
            return switchDevice.isState();
        }
        LOG.warn("unknown instance: " + deviceModel.getClass().getName());
        return false;
    }

    private boolean isAutoModeOn(AbstractDeviceModel deviceModel) {
        if(deviceModel instanceof Switch switchDevice){
            return switchDevice.getAutomation();
        }
        LOG.warn("unknown instance: " + deviceModel.getClass().getName());
        return false;
    }

    private void sortDevicesAccordingToPriority(){
        overflowControlledDevices.sort(Comparator.comparingInt(OverflowControlledDevice::defaultPriority));
    }

    private String getProperty(Field field, String name){
        return env.getProperty("pvOverflow." + field.getName() + "." + name);
    }

    private int readMaxGridWattage(String shortName){
        var state = stateHandlerDAO.readState(STATEHANDLER_GROUPNAME_PV_OVERFLOW, shortName);
        if(state != null){
            return Integer.parseInt(state.getValue());
        }
        int writeDelaySeconds = stateHandlerDAO.isSetupIsRunning()? 30 : 0;
        scheduler.schedule(() -> stateHandlerDAO.writeState(
                STATEHANDLER_GROUPNAME_PV_OVERFLOW, shortName, Integer.toString(0)), writeDelaySeconds, TimeUnit.SECONDS);
        return 0;
    }

    private record OverflowControlledDevice (
            Field field,
            String shortName,
            int defaultWattage,
            int switchOnDelay,
            int switchOffDelay,
            int defaultPriority,
            int maxDailyOnSwitching
    ) {}

    private static class OverflowControlledDeviceState {
        private int lastKnownWattage;
        private LocalDateTime controlStateTimestamp;
        private ControlState controlState;
        private int dailyOnSwitchingCounter = 0;
    }

    private enum ControlState {
        STABLE, PREPARE_TO_ON, PREPARE_TO_OFF
    }
}
