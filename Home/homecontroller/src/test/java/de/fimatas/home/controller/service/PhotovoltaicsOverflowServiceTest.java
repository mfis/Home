package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.model.PvAdditionalDataModel;
import de.fimatas.home.library.model.PvBatteryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PhotovoltaicsOverflowServiceTest {

    @InjectMocks
    private PhotovoltaicsOverflowService photovoltaicsOverflowService;

    @Mock
    private HouseService houseService;

    @Mock
    private UniqueTimestampService uniqueTimestampService;

    @Mock
    private PushService pushService;

    @Mock
    private Environment env;

    @Mock
    private StateHandlerDAO stateHandlerDAO;

    @BeforeEach
    public void setup(){
        ModelObjectDAO.resetAll();
        setProperties();
        photovoltaicsOverflowService.init();
    }

    @Test
    void testAllStayingOffCausedByAutomation() {
        refreshDevicesWithBatteryDefault(0, -3000, false, false, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(20, -3000, false, false, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testAllStayingOffCausedByPower() {
        refreshDevicesWithBatteryDefault(0, +3000, true, false, true, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(20, +3000, false, true, true, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOnByPriority() {
        refreshDevicesWithBatteryDefault(0, -2100, true, false, true, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(5, -2100, true, false, true, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, true);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testAllOnAfterDelay() {
        refreshDevicesWithBatteryDefault(0, -3000, true,  false, true, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(3, -3000, true, false, true, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, true);

        refreshDevicesWithBatteryDefault(6, -3000, true, false, true, true);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, true);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOffByPriority() {
        refreshDevicesWithBatteryDefault(0, +200, true,  true, true, true);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(20, +200, true,  true, true, true);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, false);
    }

    @Test
    void testSwitchPriority() {
        refreshDevicesWithBatteryDefault(0, -500, true,  false, true, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(10, -500, true,  false, true, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, true);

        refreshDevicesWithBatteryDefault(30, -1700, true,  false, true, true);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(50, -1700, true,  false, true, true);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, true);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, false);
    }

    @Test
    void testAllOff() {
        refreshDevicesWithBatteryDefault(0, +3000, true,  true, true, true);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(20, +3000, true,  true, true, true);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, false);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, false);
    }

    @Test
    void testOffAfterDelay() {
        refreshDevicesWithBatteryDefault(0, +500, true,  true, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(7, +500, true,  true, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(12, +500, true,  true, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, false);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOnInterrupted() {
        refreshDevicesWithBatteryDefault(0, -2200, true, false, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(3, +500, true, false, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(6, -2200, true, false, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOffInterrupted() {
        refreshDevicesWithBatteryDefault(0, +600, true, true, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(8, -200, true, true, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(11, +600, true, true, false, false);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOnCausedByHighPvBattery() {
        refreshDevicesWithBatteryDefault(0, 0, true,  false, false, false);
        refreshPvBattery(30, PvBatteryState.CHARGING, 200);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshDevicesWithBatteryDefault(10, 0, true,  false, false, false);
        refreshPvBattery(30, PvBatteryState.CHARGING, 200);
        callService();
        verifySwitch(Device.SCHALTER_WALLBOX, true);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    private void verifySwitch(Device device, Boolean state){
        if(state==null){
            verify(houseService, times(0)).togglestate(device, false);
            verify(houseService, times(0)).togglestate(device, true);
        }else{
            verify(houseService, times(1)).togglestate(device, state);
            verify(houseService, times(0)).togglestate(device, !state);
        }
    }

    private void refreshDevicesWithBatteryDefault(int minutes, int wattage, boolean wallboxAutomatic, boolean wallboxOn, boolean heatingAutomatic, boolean heatingOn) {

        Mockito.reset(houseService);
        setDateTimeOffsetMinutes(minutes);

        HouseModel houseModel = new HouseModel();

        houseModel.setGridElectricalPower(new PowerMeter());
        houseModel.getGridElectricalPower().setActualConsumption(new ValueWithTendency<>());
        houseModel.getGridElectricalPower().getActualConsumption().setValue(new BigDecimal(wattage));
        houseModel.setGridElectricStatusTime(uniqueTimestampService.getNonUnique().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        houseModel.setWallboxSwitch(new Switch());
        houseModel.getWallboxSwitch().setDevice(Device.SCHALTER_WALLBOX);
        houseModel.getWallboxSwitch().setAutomation(wallboxAutomatic);
        houseModel.getWallboxSwitch().setState(wallboxOn);
        houseModel.getWallboxSwitch().setMinPvBatteryPercentageInOverflowAutomationMode(PvBatteryMinCharge.FULL);

        houseModel.setGuestRoomInfraredHeater(new Switch());
        houseModel.getGuestRoomInfraredHeater().setDevice(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG);
        houseModel.getGuestRoomInfraredHeater().setAutomation(heatingAutomatic);
        houseModel.getGuestRoomInfraredHeater().setState(heatingOn);
        houseModel.getGuestRoomInfraredHeater().setMinPvBatteryPercentageInOverflowAutomationMode(PvBatteryMinCharge.FULL);

        ModelObjectDAO.getInstance().write(houseModel);

        refreshPvBattery(10, PvBatteryState.STABLE, 0);
    }

    private void refreshPvBattery(int soc, PvBatteryState state, int wattage) {

        var pvAdditionalDataModel = new PvAdditionalDataModel();
        pvAdditionalDataModel.setBatteryStateOfCharge(soc);
        pvAdditionalDataModel.setMinChargingWattageForOverflowControl(2000);
        pvAdditionalDataModel.setBatteryCapacity(new BigDecimal(4750));
        pvAdditionalDataModel.setMaxChargeWattage(2500);
        pvAdditionalDataModel.setPvBatteryState(state);
        pvAdditionalDataModel.setBatteryWattage(wattage);
        pvAdditionalDataModel.setBatteryPercentageEmptyForOverflowControl(5);
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel);
    }

    private void callService() {
        photovoltaicsOverflowService.houseModelRefreshed();
    }

    private void setDateTimeOffsetMinutes(int minutes) {
        when(uniqueTimestampService.getNonUnique()).thenReturn(LocalDateTime.of(2023, 10, 14, 13, 30)
                .plusMinutes(minutes));
    }

    private void setProperties() {
        // device 1
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.shortName")).thenReturn("Wallbox");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.defaultWattage")).thenReturn("2200");
        lenient().when(stateHandlerDAO.readState("pv-overflow", "Wallbox")).thenReturn(stateWithValue("220"));
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.switchOnDelay")).thenReturn("4");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.switchOffDelay")).thenReturn("10");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.defaultPriority")).thenReturn("1");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.maxDailyOnSwitching")).thenReturn("25");

        // device 2
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.shortName")).thenReturn("Hzg.Gaeste");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.defaultWattage")).thenReturn("430");
        lenient().when(stateHandlerDAO.readState("pv-overflow", "Hzg.Gaeste")).thenReturn(stateWithValue("43"));
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.switchOnDelay")).thenReturn("2");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.switchOffDelay")).thenReturn("2");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.defaultPriority")).thenReturn("2");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.maxDailyOnSwitching")).thenReturn("999");
    }

    private State stateWithValue(String value){
        var state = new State();
        state.setValue(value);
        return state;
    }
}