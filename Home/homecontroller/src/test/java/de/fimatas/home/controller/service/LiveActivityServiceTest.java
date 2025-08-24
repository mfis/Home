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
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import static de.fimatas.home.controller.service.LiveActivityService.EQUAL_MODEL_STALE_PREVENTION_DURATION;
import static org.mockito.ArgumentMatchers.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class LiveActivityServiceTest {

    @InjectMocks
    private LiveActivityService liveActivityService;

    @Mock
    private PushService pushService;

    @Mock
    private UniqueTimestampService uniqueTimestampService;

    @Mock
    private Environment env;

    @Captor
    private ArgumentCaptor<Map<String, Object>> argCaptorValueMap;

    @Captor
    private ArgumentCaptor<Boolean> argCaptorHighPriority;

    private final LocalDateTime DEFAULT_LOCAL_DATE_TIME = LocalDateTime.of(2025, 8, 24, 17, 30, 0);

    @BeforeEach
    public void setup(){
        LiveActivityDAO.getInstance().getActiveLiveActivities().clear();
        ModelObjectDAO.resetAll();
        lenient().when(uniqueTimestampService.getNonUnique()).thenReturn(DEFAULT_LOCAL_DATE_TIME);
        lenient().when(env.getProperty("liveactivity.enabled")).thenReturn("true");
    }

    @Test
    void testStart() {
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(0, PvBatteryState.STABLE, 100, 0));
        liveActivityService.start("test", "user", "device");

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());

        assertNotNull(argCaptorValueMap.getValue());
        assertNotNull(argCaptorValueMap.getValue().get("timestamp"));
        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,1kW", getSingleVal(0, "primary", "val"));
        assertEquals(".green", getSingleVal(0, "primary", "color"));
        assertEquals("0,1", getSingleVal(0, "primary", "valShort"));
        assertEquals("solarpanel", getSingleVal(0, "primary", "symbolName"));
        assertEquals("0%", getSingleVal(0, "secondary", "val"));
    }

    @Test
    void testStartNotEnabled() {

        lenient().when(env.getProperty("liveactivity.enabled")).thenReturn("false");

        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(0, PvBatteryState.STABLE, 100, 0));
        liveActivityService.start("test", "user", "device");

        verify(pushService, times(0))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());
    }

    @Test
    void testStartWithPvBattery() {
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(20, PvBatteryState.CHARGING, 100, 0));
        liveActivityService.start("test", "user", "device");

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());

        assertNotNull(argCaptorValueMap.getValue());
        assertNotNull(argCaptorValueMap.getValue().get("timestamp"));
        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,1kW", getSingleVal(0, "primary", "val"));
        assertEquals(".green", getSingleVal(0, "primary", "color"));
        assertEquals("0,1", getSingleVal(0, "primary", "valShort"));
        assertEquals("solarpanel", getSingleVal(0, "primary", "symbolName"));
        assertEquals("20%", getSingleVal(0, "secondary", "val"));
        assertEquals(".blue", getSingleVal(0, "secondary", "color"));
        assertEquals("20%", getSingleVal(0, "secondary", "valShort"));
        assertEquals("battery.25percent", getSingleVal(0, "secondary", "symbolName"));
    }

    @Test
    void testNewModelWithDifferentValue() {
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(0, PvBatteryState.STABLE, 100, 0));
        liveActivityService.start("test", "user", "device");

        liveActivityService.processModel(pvAdditionalDataModel(0, PvBatteryState.STABLE, 1000, 0));

        verify(pushService, times(2))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,1kW", getSingleVal(0, "primary", "val"));
        assertTrue(getSinglePriorityHigh(1));
        assertEquals("1,0kW", getSingleVal(1, "primary", "val"));
    }

    @Test
    void testNewModelWithSameValue() {
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(0, PvBatteryState.STABLE, 500, 0));
        liveActivityService.start("test", "user", "device");

        lenient().when(uniqueTimestampService.getNonUnique()).thenReturn(DEFAULT_LOCAL_DATE_TIME.plusMinutes(1));

        liveActivityService.processModel(pvAdditionalDataModel(0, PvBatteryState.STABLE, 500, 0));

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,5kW", getSingleVal(0, "primary", "val"));
    }


    @Test
    void testNewModelWithSameValuePreventingStale() {
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(0, PvBatteryState.STABLE, 500, 0));
        liveActivityService.start("test", "user", "device");

        lenient().when(uniqueTimestampService.getNonUnique())
                .thenReturn(DEFAULT_LOCAL_DATE_TIME.plus(EQUAL_MODEL_STALE_PREVENTION_DURATION).plusSeconds(1));

        liveActivityService.processModel(pvAdditionalDataModel(0, PvBatteryState.STABLE, 500, 0));

        verify(pushService, times(2))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,5kW", getSingleVal(0, "primary", "val"));
    }

    @Test
    void testNewModelWithSimilarValue() {
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(0, PvBatteryState.STABLE, 500, 0));
        liveActivityService.start("test", "user", "device");

        liveActivityService.processModel(pvAdditionalDataModel(0, PvBatteryState.STABLE, 510, 0));
        liveActivityService.processModel(pvAdditionalDataModel(0, PvBatteryState.STABLE, 490, 0));
        liveActivityService.processModel(pvAdditionalDataModel(0, PvBatteryState.STABLE, 500, 0));

        verify(pushService, times(3))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,5kW", getSingleVal(0, "primary", "val"));
        assertFalse(getSinglePriorityHigh(1));
        assertEquals("0,5kW", getSingleVal(1, "primary", "val"));
        assertFalse(getSinglePriorityHigh(2));
        assertEquals("0,5kW", getSingleVal(2, "primary", "val"));
    }

    @Test
    void testNewModelWithSimilarValueButDifferentSign() {
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(0, PvBatteryState.STABLE, 5, 0));
        liveActivityService.start("test", "user", "device");

        liveActivityService.processModel(pvAdditionalDataModel(0, null, -2, 0));

        verify(pushService, times(2))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,0kW", getSingleVal(0, "primary", "val"));
        assertEquals(".green", getSingleVal(0, "primary", "color"));
        assertTrue(getSinglePriorityHigh(1));
        assertEquals("0,0kW", getSingleVal(1, "primary", "val"));
        assertEquals(".white", getSingleVal(1, "primary", "color"));
    }


    @Test
    void testStartHouseAndElectricVehicleNoCharge() {
        ModelObjectDAO.getInstance().write(pvAdditionalDataModel(0, PvBatteryState.STABLE, 500, 0));
        ModelObjectDAO.getInstance().write(electricVehicleModelWithValue(30, 0));
        liveActivityService.start("test", "user", "device");

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), argCaptorHighPriority.capture(), eq(false), any(Duration.class), any(Instant.class), argCaptorValueMap.capture());

        assertTrue(getSinglePriorityHigh(0));
        assertEquals("0,5kW", getSingleVal(0, "primary", "val"));
        assertEquals("0%", getSingleVal(0, "secondary", "val"));
    }

    private boolean getSinglePriorityHigh(int number) {
        return argCaptorHighPriority.getAllValues().get(number);
    }

    private String getSingleVal(int number, String mapName, String name) {
        //noinspection unchecked
        return (String)((Map<String, Object>) argCaptorValueMap.getAllValues().get(number).get(mapName)).get(name);
    }

    private PvAdditionalDataModel pvAdditionalDataModel(int soc, PvBatteryState pvBatteryState, int production, int consumption) {
        var pvAdditionalDataModel = new PvAdditionalDataModel();
        pvAdditionalDataModel.setLastCollectionTimeReadMillis(new Date().getTime());
        pvAdditionalDataModel.setBatteryStateOfCharge(soc);
        pvAdditionalDataModel.setMinChargingWattageForOverflowControl(2000);
        pvAdditionalDataModel.setBatteryCapacity(new BigDecimal(4750));
        pvAdditionalDataModel.setMaxChargeWattage(2500);
        pvAdditionalDataModel.setPvBatteryState(pvBatteryState);
        pvAdditionalDataModel.setBatteryWattage(0);
        pvAdditionalDataModel.setBatteryPercentageEmptyForOverflowControl(5);
        pvAdditionalDataModel.setProductionWattage(production);
        pvAdditionalDataModel.setConsumptionWattage(consumption);
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