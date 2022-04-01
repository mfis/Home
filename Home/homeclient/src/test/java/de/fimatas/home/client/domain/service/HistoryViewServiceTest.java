package de.fimatas.home.client.domain.service;

import de.fimatas.home.client.domain.model.HistoryEntry;
import de.fimatas.home.library.domain.model.PowerConsumptionMonth;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryViewServiceTest {

    @Test
    void testCalculatePreviousYearDifference(){

        assertEquals("+135%", calculatePreviousYearDifference(40L, 94L));
        assertEquals("+100%", calculatePreviousYearDifference(10L, 20L));
        assertEquals("-50%", calculatePreviousYearDifference(50L, 25L));

        assertEquals("+20%", calculatePreviousYearDifference(50L, 60L));
        assertEquals("-17%", calculatePreviousYearDifference(60L, 50L));

        assertEquals("≈", calculatePreviousYearDifference(50L, 50L));

        assertEquals("", calculatePreviousYearDifference(null, 50L));
        assertEquals("", calculatePreviousYearDifference(0L, 50L));

        assertEquals("-100%", calculatePreviousYearDifference(50L, null));
        assertEquals("-100%", calculatePreviousYearDifference(50L, 0L));
    }

    private String calculatePreviousYearDifference(Long oldValue, Long newValue){

        var testObject = new HistoryEntry();
        var actualValue = new PowerConsumptionMonth();
        actualValue.setMeasurePointMax(LocalDate.of(2022, 3, 31).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        actualValue.setPowerConsumption( newValue);

        var oldPowerConsumptionMonth = new PowerConsumptionMonth();
        oldPowerConsumptionMonth.setMeasurePointMax(LocalDate.of(2021, 3, 31).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        oldPowerConsumptionMonth.setPowerConsumption( oldValue);

        new HistoryViewService().calculatePreviousYearDifference(testObject, actualValue, List.of(oldPowerConsumptionMonth), null);

        return testObject.getBadgeValue();
    }
}