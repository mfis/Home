package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.LiveActivityDAO;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.PowerMeter;
import de.fimatas.home.library.domain.model.ValueWithTendency;
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

    @BeforeEach
    public void setup(){
        LiveActivityDAO.getInstance().getActiveLiveActivities().clear();
    }

    @Test
    void testStart() {
        ModelObjectDAO.getInstance().write(houseModelWithElectricGridValue(100));
        liveActivityService.start("test", "user", "device");
        //liveActivityService.newModel(houseModelWithElectricGridValue(1000));

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), eq(true), eq(false), argCaptorValueMap.capture());

        assertNotNull(argCaptorValueMap.getValue());
        assertNotNull(argCaptorValueMap.getValue().get("timestamp"));
        assertEquals("100W", getSingleVal(0, "primary", "val"));
        assertEquals(".orange", getSingleVal(0, "primary", "color"));
        assertEquals("0,1", getSingleVal(0, "primary", "valShort"));
        assertEquals("energygrid", getSingleVal(0, "primary", "symbolName"));
        assertEquals("", getSingleVal(0, "secondary", "val"));
    }

    @Test
    void testNewModelWithDifferentValue() {
        ModelObjectDAO.getInstance().write(houseModelWithElectricGridValue(100));
        liveActivityService.start("test", "user", "device");

        liveActivityService.newModel(houseModelWithElectricGridValue(1000));

        verify(pushService, times(2))
                .sendLiveActivityToApns(eq("test"), eq(true), eq(false), argCaptorValueMap.capture());

        assertEquals("100W", getSingleVal(0, "primary", "val"));
        assertEquals("1000W", getSingleVal(1, "primary", "val"));
    }

    @Test
    void testNewModelWithSameValue() {
        ModelObjectDAO.getInstance().write(houseModelWithElectricGridValue(500));
        liveActivityService.start("test", "user", "device");

        liveActivityService.newModel(houseModelWithElectricGridValue(500));

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), eq(true), eq(false), argCaptorValueMap.capture());

        assertEquals("500W", getSingleVal(0, "primary", "val"));
    }

    @Test
    void testNewModelWithSimilarValue() {
        ModelObjectDAO.getInstance().write(houseModelWithElectricGridValue(500));
        liveActivityService.start("test", "user", "device");

        liveActivityService.newModel(houseModelWithElectricGridValue(510));
        liveActivityService.newModel(houseModelWithElectricGridValue(490));
        liveActivityService.newModel(houseModelWithElectricGridValue(500));

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), eq(true), eq(false), argCaptorValueMap.capture());

        assertEquals("500W", getSingleVal(0, "primary", "val"));
    }

    @Test
    void testNewModelWithSimilarValueButDifferentSign() {
        ModelObjectDAO.getInstance().write(houseModelWithElectricGridValue(5));
        liveActivityService.start("test", "user", "device");

        liveActivityService.newModel(houseModelWithElectricGridValue(-2));

        verify(pushService, times(2))
                .sendLiveActivityToApns(eq("test"), eq(true), eq(false), argCaptorValueMap.capture());

        assertEquals("5W", getSingleVal(0, "primary", "val"));
        assertEquals(".orange", getSingleVal(0, "primary", "color"));
        assertEquals("2W", getSingleVal(1, "primary", "val"));
        assertEquals(".green", getSingleVal(1, "primary", "color"));
    }

    private Object getSingleVal(int number, String mapName, String name) {
        //noinspection unchecked
        return ((Map<String, Object>) argCaptorValueMap.getAllValues().get(number).get(mapName)).get(name);
    }

    private HouseModel houseModelWithElectricGridValue(int value) {
        HouseModel houseModel = new HouseModel();
        houseModel.setGridElectricalPower(new PowerMeter());
        houseModel.getGridElectricalPower().setActualConsumption(new ValueWithTendency<>());
        houseModel.getGridElectricalPower().getActualConsumption().setValue(new BigDecimal(value));
        return houseModel;
    }
}