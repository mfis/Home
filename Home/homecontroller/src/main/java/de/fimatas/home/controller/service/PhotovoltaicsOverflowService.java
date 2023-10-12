package de.fimatas.home.controller.service;


import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.annotation.EnablePhotovoltaicsOverflow;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

@Component
public class PhotovoltaicsOverflowService {

    @Autowired
    private HouseService houseService;

    private LinkedList<OverflowControlledDevice> overflowControlledDevices = new LinkedList<>();

    private Map<OverflowControlledDevice, OverflowControlledDeviceState> overflowControlledDeviceStates = new HashMap<>();

    private static final Log LOG = LogFactory.getLog(PhotovoltaicsOverflowService.class);

    @PostConstruct
    public void init() {
        for(Field field : HouseModel.class.getDeclaredFields()){
            final var enablePhotovoltaicsOverflow = field.getAnnotation(EnablePhotovoltaicsOverflow.class);
            if(enablePhotovoltaicsOverflow != null){
                final var overflowControlledDevice =
                        new OverflowControlledDevice(enablePhotovoltaicsOverflow, field);
                overflowControlledDevices.add(overflowControlledDevice);
                var ocds = new OverflowControlledDeviceState();
                ocds.lastKnownWattage = enablePhotovoltaicsOverflow.defaultWattage();
                ocds.controlState = ControlState.STABLE;
                ocds.controlStateTimestamp = LocalDateTime.now();
                overflowControlledDeviceStates.put(overflowControlledDevice, ocds);
            }
        }
        sortDevicesAccordingToPriority();
    }

    @Async
    public void houseModelRefreshed() {
        final var houseModel = ModelObjectDAO.getInstance().readHouseModel();
        var hasToRefreshHouseModel = false;

        if(houseModel.getGridElectricalPower() == null
                || houseModel.getGridElectricalPower().getActualConsumption() == null
                || houseModel.getGridElectricalPower().getActualConsumption().getValue() == null){
            return;
        }

        int wattage = houseModel.getGridElectricalPower().getActualConsumption().getValue().intValue();

        // assume grid-powered - turn something off? starting with lowest priotity
        var reverseIterator = new ReverseListIterator<OverflowControlledDevice>(overflowControlledDevices);
        while (reverseIterator.hasNext()) {
            var ocd = reverseIterator.next();
            if(wattage >= 0) {
                final var deviceModel = getDeviceModel(houseModel, ocd);
                final var actualDeviceWattage = getActualDeviceWattage(ocd, deviceModel);
                if (isAutoModeOn(deviceModel) && getActualDeviceSwitchState(deviceModel)) {
                    var maxWattsFromGrid = actualDeviceWattage * ocd.attributes.percentageMaxPowerFromGrid() / 100;
                    if (wattage > maxWattsFromGrid) { // FIXME: delay
                        houseService.togglestate(deviceModel.getDevice(), false);
                        hasToRefreshHouseModel = true;
                        wattage -= actualDeviceWattage;
                    }
                }
            }
        }

        if(wattage < 0){
            // assume pv overflow - turn something on? starting with highest priority
            // FIXME
        }

        if(hasToRefreshHouseModel){
            houseService.refreshHouseModel();
        }
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

    private boolean getActualDeviceSwitchState(AbstractDeviceModel deviceModel) {
        if(deviceModel instanceof Switch switchDevice){
            return switchDevice.isState();
        }
        return false;
    }

    private boolean isAutoModeOn(AbstractDeviceModel deviceModel) {
        if(deviceModel instanceof Switch switchDevice){
            return switchDevice.getAutomation();
        }
        return false;
    }

    private void sortDevicesAccordingToPriority(){
        // overflowControlledDevices.sort(...); TODO
    }

    private record OverflowControlledDevice (
            EnablePhotovoltaicsOverflow attributes,
            Field field
    ) {}

    private class OverflowControlledDeviceState {
        private int lastKnownWattage;
        private LocalDateTime controlStateTimestamp;
        private ControlState controlState;
    }

    private enum ControlState {
        STABLE, PREPARE_TO_ON, PREPARE_TO_OFF
    }
}
