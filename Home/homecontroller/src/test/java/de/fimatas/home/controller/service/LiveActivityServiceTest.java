package de.fimatas.home.controller.service;

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

    @Test
    void testSart() {
        ModelObjectDAO.getInstance().write(houseModelWithElectricGridValue(100));
        liveActivityService.start("test", "user", "device");
        //liveActivityService.newModel(houseModelWithElectricGridValue(1000));

        verify(pushService, times(1))
                .sendLiveActivityToApns(eq("test"), eq(true), eq(false), argCaptorValueMap.capture());

        assertNotNull(argCaptorValueMap.getValue());
        assertNotNull(argCaptorValueMap.getValue().get("timestamp"));
        assertEquals("100W", getVal("primary"));
        assertEquals("", getVal("secondary"));
    }

    private Object getVal(String name) {
        //noinspection unchecked
        return ((Map<String, Object>) argCaptorValueMap.getValue().get(name)).get("val");
    }

    private HouseModel houseModelWithElectricGridValue(int value) {
        HouseModel houseModel = new HouseModel();
        houseModel.setGridElectricalPower(new PowerMeter());
        houseModel.getGridElectricalPower().setActualConsumption(new ValueWithTendency<>());
        houseModel.getGridElectricalPower().getActualConsumption().setValue(new BigDecimal(value));
        return houseModel;
    }
}