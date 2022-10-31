package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.domain.service.HistoryService;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.controller.service.*;
import de.fimatas.home.library.domain.model.ElectricVehicle;
import de.fimatas.home.library.domain.model.EvChargePoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EvChargingDAOTest {

    @Autowired
    private EvChargingDAO evChargingDAO;

    @MockBean
    private HouseService houseService;

    @MockBean
    private LightService lightService;

    @MockBean
    private PresenceService presenceService;

    @MockBean
    private ElectricVehicleService electricVehicleService;

    @MockBean
    private WeatherService weatherService;

    @MockBean
    private HistoryService historyService;

    @MockBean
    private SettingsService settingsService;

    @MockBean
    private ClientCommunicationService clientCommunicationService;

    @MockBean
    private PushService pushService;

    @Test
    void testSimpleWriteRead(){
        final LocalDateTime percentageSetTS = LocalDateTime.now();
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(1), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER, percentageSetTS);

        assertNotNull(read);
        assertEquals(1, read.size());
        assertFalse(read.get(0).finished());
        assertEquals(0, read.get(0).countValueAsKWH().intValue());
    }

    @Test
    void testAddSomeValues(){
        final LocalDateTime percentageSetTS = LocalDateTime.now();
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(1), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(2), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(3), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER, percentageSetTS);

        assertNotNull(read);
        assertEquals(1, read.size());
        assertEquals(2, read.get(0).countValueAsKWH().intValue());
    }

    @Test // should be handled in service
    void testPercentageSetAfterChargingStart() throws Exception{
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(1), EvChargePoint.WALLBOX1);
        Thread.sleep(100L);
        final LocalDateTime percentageSetTS = LocalDateTime.now();
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(2), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(3), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER, percentageSetTS);

        assertNotNull(read);
        assertEquals(0, read.size());
    }

    @Test // should be handled in service
    void testFinished(){
        final LocalDateTime percentageSetTS = LocalDateTime.now();
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(1), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(2), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER, percentageSetTS);

        assertNotNull(read);
        assertEquals(1, read.size());
        assertTrue(read.get(0).finished());
    }
}