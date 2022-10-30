package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.model.EvChargeDatabaseEntry;
import de.fimatas.home.library.domain.model.ElectricVehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EvChargingDAOTest {

    @Autowired
    private EvChargingDAO evChargingDAO;


    @Test
    void testSimpleWrite(){
        evChargingDAO.write(ElectricVehicle.OTHER, new BigDecimal(1), false);
        final List<EvChargeDatabaseEntry> read = evChargingDAO.read(ElectricVehicle.OTHER, LocalDateTime.now());
        assertNotNull(read);
        assertEquals(1, read.size());
    }

}