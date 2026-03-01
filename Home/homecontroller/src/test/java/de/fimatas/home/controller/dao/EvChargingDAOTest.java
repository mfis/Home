package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.domain.service.HistoryService;
import de.fimatas.home.controller.domain.service.HouseService;
import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.controller.service.*;
import de.fimatas.home.library.domain.model.ElectricVehicle;
import de.fimatas.home.library.domain.model.EvChargePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EvChargingDAOTest {

    @Autowired
    private EvChargingDAO evChargingDAO;

    @Autowired
    private UniqueTimestampService uniqueTimestampService;

    @MockitoBean
    private HouseService houseService;

    @MockitoBean
    private LightService lightService;

    @MockitoBean
    private PresenceService presenceService;

    @MockitoBean
    private ElectricVehicleService electricVehicleService;

    @MockitoBean
    private WeatherService weatherService;

    @MockitoBean
    private HistoryService historyService;

    @MockitoBean
    private SettingsService settingsService;

    @MockitoBean
    private ClientCommunicationService clientCommunicationService;

    @MockitoBean
    private PushService pushService;

    @BeforeEach
    public void beforeEach(){
        evChargingDAO.completeInit();
    }

    @Test
    void testSimpleWriteRead(){
        final LocalDateTime percentageSetTS = uniqueTimestampService.get();
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(1000), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER_EV, percentageSetTS);

        assertNotNull(read);
        assertEquals(1, read.size());
        assertFalse(read.get(0).finished());
        assertEquals(0, read.get(0).countValueAsKWH().intValue());
    }

    @Test
    void testAddSomeValues(){
        final LocalDateTime percentageSetTS = uniqueTimestampService.get();
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(1000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(2000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(3000), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER_EV, percentageSetTS);

        assertNotNull(read);
        assertEquals(1, read.size());
        assertEquals(2, read.get(0).countValueAsKWH().intValue());
    }

    @Test // should be handled in service
    void testPercentageSetAfterChargingStart() {
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(1000), EvChargePoint.WALLBOX1);
        final LocalDateTime percentageSetTS = uniqueTimestampService.get();
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(2000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(3000), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER_EV, percentageSetTS);

        assertNotNull(read);
        assertEquals(0, read.size());
    }

    @Test // should be handled in service
    void testFinished(){
        final LocalDateTime percentageSetTS = uniqueTimestampService.get();
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(1000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(2000), EvChargePoint.WALLBOX1);
        evChargingDAO.finishAll();
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER_EV, percentageSetTS);

        assertNotNull(read);
        assertEquals(1, read.size());
        assertTrue(read.get(0).finished());
    }

    @Test
    void testOverflow(){
        final LocalDateTime percentageSetTS = uniqueTimestampService.get();
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(1000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(2000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(100), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(200), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER_EV, percentageSetTS);

        assertNotNull(read);
        assertEquals(1, read.size());
        assertEquals(1, read.get(0).countValueAsKWH().intValue());
        assertEquals(1000, read.get(0).getStartVal().intValue());
        assertEquals(200, read.get(0).getEndVal().intValue());
        assertEquals(2000, read.get(0).getMaxVal().intValue());
    }

    @Test
    void testOverflowKWH(){
        final LocalDateTime percentageSetTS = uniqueTimestampService.get();
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(1000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(2000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(3000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(100), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(1000), EvChargePoint.WALLBOX1);
        evChargingDAO.write(ElectricVehicle.OTHER_EV, new BigDecimal(2000), EvChargePoint.WALLBOX1);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER_EV, percentageSetTS);

        assertNotNull(read);
        assertEquals(1, read.size());
        assertEquals(4, read.get(0).countValueAsKWH().intValue());
    }
}