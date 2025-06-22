package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.LiveActivityDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.model.PvAdditionalDataModel;
import de.fimatas.home.library.model.PvBatteryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class LiveActivityServiceTest {

    @InjectMocks
    private LiveActivityService liveActivityService;

    @Mock
    private PushService pushService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> argCaptorValueMap;

    @Captor
    private ArgumentCaptor<Boolean> argCaptorHighPriority;

    @BeforeEach
    public void setup(){
        LiveActivityDAO.getInstance().getActiveLiveActivities().clear();
        ModelObjectDAO.resetAll();
    }

    @Test
    void testStart() {
        ModelObjectDAO.getInstance().write(houseModelWithPvProduction(100));
        liveActivityService.start("test", "user", "device");

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), argCaptorValueMap.capture());

        assertNotNull(argCaptorValueMap.getValue());
        assertNotNull(argCaptorValueMap.getValue().get("timestamp"));
        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,1kW", getSingleVal(0, "primary", "val"));
        assertEquals(".green", getSingleVal(0, "primary", "color"));
        assertEquals("0,1", getSingleVal(0, "primary", "valShort"));
        assertEquals("sun.max.fill", getSingleVal(0, "primary", "symbolName"));
        assertEquals("", getSingleVal(0, "secondary", "val"));
    }

    @Test
    void testStartWithPvBattery() {
        ModelObjectDAO.getInstance().write(houseModelWithPvProduction(100));
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(20, PvBatteryState.CHARGING));
        liveActivityService.start("test", "user", "device");

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), argCaptorValueMap.capture());

        assertNotNull(argCaptorValueMap.getValue());
        assertNotNull(argCaptorValueMap.getValue().get("timestamp"));
        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,1kW", getSingleVal(0, "primary", "val"));
        assertEquals(".green", getSingleVal(0, "primary", "color"));
        assertEquals("0,1", getSingleVal(0, "primary", "valShort"));
        assertEquals("sun.max.fill", getSingleVal(0, "primary", "symbolName"));
        assertEquals("20%", getSingleVal(0, "tertiary", "val"));
        assertEquals(".blue", getSingleVal(0, "tertiary", "color"));
        assertEquals("20%", getSingleVal(0, "tertiary", "valShort"));
        assertEquals("battery.25percent", getSingleVal(0, "tertiary", "symbolName"));
    }

    @Test
    void testNewModelWithDifferentValue() {
        ModelObjectDAO.getInstance().write(houseModelWithPvProduction(100));
        liveActivityService.start("test", "user", "device");

        liveActivityService.newModel(houseModelWithPvProduction(1000));

        verify(pushService, times(2))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,1kW", getSingleVal(0, "primary", "val"));
        assertTrue(getSinglePriorityHigh(1));
        assertEquals("1,0kW", getSingleVal(1, "primary", "val"));
    }

    @Test
    void testNewModelWithSameValue() {
        ModelObjectDAO.getInstance().write(houseModelWithPvProduction(500));
        liveActivityService.start("test", "user", "device");

        liveActivityService.newModel(houseModelWithPvProduction(500));

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,5kW", getSingleVal(0, "primary", "val"));
    }

    @Test
    void testNewModelWithSimilarValue() {
        ModelObjectDAO.getInstance().write(houseModelWithPvProduction(500));
        liveActivityService.start("test", "user", "device");

        liveActivityService.newModel(houseModelWithPvProduction(510));
        liveActivityService.newModel(houseModelWithPvProduction(490));
        liveActivityService.newModel(houseModelWithPvProduction(500));

        verify(pushService, times(3))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,5kW", getSingleVal(0, "primary", "val"));
        assertFalse(getSinglePriorityHigh(1));
        assertEquals("0,5kW", getSingleVal(1, "primary", "val"));
        assertFalse(getSinglePriorityHigh(2));
        assertEquals("0,5kW", getSingleVal(2, "primary", "val"));
    }

    @Test
    void testNewModelWithSimilarValueButDifferentSign() {
        ModelObjectDAO.getInstance().write(houseModelWithPvProduction(5));
        liveActivityService.start("test", "user", "device");

        liveActivityService.newModel(houseModelWithPvProduction(-2));

        verify(pushService, times(2))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,0kW", getSingleVal(0, "primary", "val"));
        assertEquals(".green", getSingleVal(0, "primary", "color"));
        assertTrue(getSinglePriorityHigh(1));
        assertEquals("0,0kW", getSingleVal(1, "primary", "val"));
        assertEquals(".white", getSingleVal(1, "primary", "color"));
    }


    @Test
    void testStartHouseAndElectricVehicleNoCharge() {
        ModelObjectDAO.getInstance().write(houseModelWithPvProduction(500));
        ModelObjectDAO.getInstance().write(electricVehicleModelWithValue(30, 0));
        liveActivityService.start("test", "user", "device");

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,5kW", getSingleVal(0, "primary", "val"));
        assertEquals("", getSingleVal(0, "secondary", "val"));
    }

    private boolean getSinglePriorityHigh(int number) {
        return argCaptorHighPriority.getAllValues().get(number);
    }

    private String getSingleVal(int number, String mapName, String name) {
        //noinspection unchecked
        return (String)((Map<String, Object>) argCaptorValueMap.getAllValues().get(number).get(mapName)).get(name);
    }

    private HouseModel houseModelWithPvProduction(int value) {
        HouseModel houseModel = new HouseModel();
        houseModel.setProducedElectricalPower(new PowerMeter());
        houseModel.getProducedElectricalPower().setActualConsumption(new ValueWithTendency<>());
        houseModel.getProducedElectricalPower().getActualConsumption().setValue(new BigDecimal(value));
        return houseModel;
    }

    private PvAdditionalDataModel pvAdditionalDataModel(int soc, PvBatteryState pvBatteryState) {
        var pvAdditionalDataModel = new PvAdditionalDataModel();
        pvAdditionalDataModel.setBatteryStateOfCharge(soc);
        pvAdditionalDataModel.setMinChargingWattageForOverflowControl(2000);
        pvAdditionalDataModel.setBatteryCapacity(new BigDecimal(4750));
        pvAdditionalDataModel.setMaxChargeWattage(2500);
        pvAdditionalDataModel.setPvBatteryState(pvBatteryState);
        pvAdditionalDataModel.setBatteryWattage(0);
        pvAdditionalDataModel.setBatteryPercentageEmptyForOverflowControl(5);
        return pvAdditionalDataModel;
    }

    private ElectricVehicleModel electricVehicleModelWithValue(int base, int charge){
        ElectricVehicleModel electricVehicleModel = new ElectricVehicleModel();
        ElectricVehicleState evs = new ElectricVehicleState(ElectricVehicle.EUP, (short)base, LocalDateTime.now());
        evs.setActiveCharging(charge != 0);
        evs.setConnectedToWallbox(charge != 0);
        evs.setAdditionalChargingPercentage((short)charge);
        evs.setChargingTimestamp(charge != 0 ? LocalDateTime.now() : null);
        electricVehicleModel.getEvMap().put(ElectricVehicle.EUP, evs);
        return electricVehicleModel;
    }
}