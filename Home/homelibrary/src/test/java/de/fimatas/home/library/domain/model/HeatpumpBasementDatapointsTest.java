package de.fimatas.home.library.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatpumpBasementDatapointsTest {

    @Test
    void valueAsBigDecimal() {
        assertEquals(new BigDecimal("25"), HeatpumpBasementDatapoints.valueAsBigDecimal("25"));
        assertEquals(new BigDecimal("25"), HeatpumpBasementDatapoints.valueAsBigDecimal("  25  "));
        assertEquals(new BigDecimal("25.5"), HeatpumpBasementDatapoints.valueAsBigDecimal("25,5"));
        assertEquals(new BigDecimal("25.5"), HeatpumpBasementDatapoints.valueAsBigDecimal("25,5 °C"));
        assertEquals(new BigDecimal("1234"), HeatpumpBasementDatapoints.valueAsBigDecimal("1.234 kW/h"));
    }
}