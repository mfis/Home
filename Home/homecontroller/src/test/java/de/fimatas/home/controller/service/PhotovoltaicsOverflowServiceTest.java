package de.fimatas.home.controller.service;

import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.*;
import de.fimatas.home.library.homematic.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private Environment env;

    @BeforeEach
    public void setup(){
        ModelObjectDAO.resetAll();
        setProperties();
        photovoltaicsOverflowService.init();
    }

    @Test
    void testAllOff() {
        refreshHouseModelWithMinutesWattageAndAutomation(0, -3000, false, false);
        verifySwitch(Device.SCHALTER_WALLBOX, null);
        verifySwitch(Device.SCHALTER_GAESTEZIMMER_INFRAROTHEIZUNG, null);

        refreshHouseModelWithMinutesWattageAndAutomation(20, -3000, false, false);
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

    private void refreshHouseModelWithMinutesWattageAndAutomation(int minutes, int wattage, boolean wallboxAutomatic, boolean heatingAutomatic) {
        setDateTimeOffsetMinutes(minutes);
        HouseModel houseModel = new HouseModel();
        houseModel.setGridElectricalPower(new PowerMeter());
        houseModel.getGridElectricalPower().setActualConsumption(new ValueWithTendency<>());
        houseModel.getGridElectricalPower().getActualConsumption().setValue(new BigDecimal(wattage));
        houseModel.setGridElectricStatusTime(uniqueTimestampService.getNonUnique().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        houseModel.setWallboxSwitch(new Switch());
        houseModel.getWallboxSwitch().setAutomation(wallboxAutomatic);
        houseModel.setGuestRoomInfraredHeater(new Switch());
        houseModel.getGuestRoomInfraredHeater().setAutomation(heatingAutomatic);
        ModelObjectDAO.getInstance().write(houseModel);
        photovoltaicsOverflowService.houseModelRefreshed();
    }

    private void setDateTimeOffsetMinutes(int minutes) {
        when(uniqueTimestampService.getNonUnique()).thenReturn(LocalDateTime.of(2023, 10, 14, 13, 30)
                .plusMinutes(minutes));
    }

    private void setProperties() {
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.shortName")).thenReturn("Wallbox");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.defaultWattage")).thenReturn("2200");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.percentageMaxPowerFromGrid")).thenReturn("10");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.switchOnDelay")).thenReturn("4");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.switchOffDelay")).thenReturn("10");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.defaultPriority")).thenReturn("1");
        lenient().when(env.getProperty("pvOverflow.wallboxSwitch.maxDailyOnSwitching")).thenReturn("25");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.shortName")).thenReturn("Hzg.Gaeste");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.defaultWattage")).thenReturn("430");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.percentageMaxPowerFromGrid")).thenReturn("10");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.switchOnDelay")).thenReturn("2");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.switchOffDelay")).thenReturn("2");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.defaultPriority")).thenReturn("2");
        lenient().when(env.getProperty("pvOverflow.guestRoomInfraredHeater.maxDailyOnSwitching")).thenReturn("999");
    }

}