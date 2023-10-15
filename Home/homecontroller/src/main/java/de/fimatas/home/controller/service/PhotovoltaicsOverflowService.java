package de.fimatas.home.controller.service;

import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.annotation.EnablePhotovoltaicsOverflow;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
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

@Component
public class PhotovoltaicsOverflowService {

    @Autowired
    private HouseService houseService;

    @Autowired
    private PushService pushService;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @Autowired
    private Environment env;

    private final LinkedList<OverflowControlledDevice> overflowControlledDevices = new LinkedList<>();

    private final Map<OverflowControlledDevice, OverflowControlledDeviceState> overflowControlledDeviceStates = new HashMap<>();

    private long lastGridElectricStatusTime = -1;

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
                                Integer.parseInt(getProperty(field, "percentageMaxPowerFromGrid")),
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

    private String getProperty(Field field, String name){
        return env.getProperty("pvOverflow." + field.getName() + "." + name);
    }

    @Scheduled(cron = "0 00 00 * * *")
    public void resetCounter() {
        overflowControlledDeviceStates.values().forEach(ocds -> ocds.dailyOnSwitchingCounter = 0);
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
                    var maxWattsFromGrid = actualDeviceWattage * ocd.percentageMaxPowerFromGrid / 100;
                    if (wattage > maxWattsFromGrid) {
                        switch(overflowControlledDeviceStates.get(ocd).controlState){
                            case STABLE -> setControlState(ocd, ControlState.PREPARE_TO_OFF);
                            case PREPARE_TO_OFF -> {
                                if(isSwitchOffDelayReached(ocd)){
                                    LOG.info("switch OFF " + deviceModel.getDevice().name());
                                    houseService.togglestate(deviceModel.getDevice(), false);
                                    hasToRefreshHouseModel = true;
                                    wattage -= actualDeviceWattage;
                                    pushService.sendNotice("PV-Überschuss: " + deviceModel.getDevice().getDescription() + " ausgeschaltet.");
                                }
                            }
                            case PREPARE_TO_ON -> LOG.warn("state confusion (check off)!");
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
                    var minWattsFromPV = (actualDeviceWattage - (actualDeviceWattage * ocd.percentageMaxPowerFromGrid / 100)) * -1;
                    if (wattage <= minWattsFromPV) {
                        switch (overflowControlledDeviceStates.get(ocd).controlState) {
                            case STABLE -> setControlState(ocd, ControlState.PREPARE_TO_ON);
                            case PREPARE_TO_ON -> {
                                if (isSwitchOnDelayReachedAndAllowed(ocd)) {
                                    LOG.info("switch ON " + deviceModel.getDevice().name() + " #" +
                                            overflowControlledDeviceStates.get(ocd).dailyOnSwitchingCounter + "/" + ocd.maxDailyOnSwitching);
                                    houseService.togglestate(deviceModel.getDevice(), true);
                                    hasToRefreshHouseModel = true;
                                    overflowControlledDeviceStates.get(ocd).dailyOnSwitchingCounter += 1;
                                    wattage += actualDeviceWattage;
                                    pushService.sendNotice("PV-Überschuss: " + deviceModel.getDevice().getDescription() + " eingeschaltet.");
                                }
                            }
                            case PREPARE_TO_OFF -> LOG.warn("state confusion (check on)!");
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
            houseService.refreshHouseModel();
        }
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

    private record OverflowControlledDevice (
            Field field,
            String shortName,
            int defaultWattage,
            int percentageMaxPowerFromGrid,
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
