package de.fimatas.home.controller.service;

import de.fimatas.home.controller.dao.StateHandlerDAO;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.controller.model.State;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Device;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
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
        refresh(0, -3000, false, false, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(20, -3000, false, false, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testAllStayingOffCausedByPower() {
        refresh(0, +3000, true, false, true, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(20, +3000, false, true, true, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOnByPriority() {
        refresh(0, -2100, true, false, true, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(5, -2100, true, false, true, false);
        verifySwitch(Device.SCHALTER_WALLBOX, true);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testAllOnAfterDelay() {
        refresh(0, -3000, true,  false, true, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(3, -3000, true, false, true, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, true);

        refresh(6, -3000, true, false, true, true);
        verifySwitch(Device.SCHALTER_WALLBOX, true);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOffByPriority() {
        refresh(0, +200, true,  true, true, true);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(20, +200, true,  true, true, true);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, false);
    }

    @Test
    void testSwitchPriority() {
        refresh(0, -500, true,  false, true, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(10, -500, true,  false, true, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, true);

        refresh(30, -1700, true,  false, true, true);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(50, -1700, true,  false, true, true);
        verifySwitch(Device.SCHALTER_WALLBOX, true);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, false);
    }

    @Test
    void testAllOff() {
        refresh(0, +3000, true,  true, true, true);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(20, +3000, true,  true, true, true);
        verifySwitch(Device.SCHALTER_WALLBOX, false);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, false);
    }

    @Test
    void testOffAfterDelay() {
        refresh(0, +500, true,  true, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(7, +500, true,  true, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(12, +500, true,  true, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, false);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOnInterrupted() {
        refresh(0, -2200, true, false, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(3, +500, true, false, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(6, -2200, true, false, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);
    }

    @Test
    void testOffInterrupted() {
        refresh(0, +600, true, true, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(8, -200, true, true, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refresh(11, +600, true, true, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
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

    private void refresh(int minutes, int wattage, boolean wallboxAutomatic, boolean wallboxOn, boolean heatingAutomatic, boolean heatingOn) {
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
        houseModel.setGuestRoomInfraredHeater(new Switch());
        houseModel.getGuestRoomInfraredHeater().setDevice(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG);
        houseModel.getGuestRoomInfraredHeater().setAutomation(heatingAutomatic);
        houseModel.getGuestRoomInfraredHeater().setState(heatingOn);
        ModelObjectDAO.getInstance().write(houseModel);
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