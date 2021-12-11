package de.fimatas.home.client.domain.service;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

public class HouseViewServiceTest {

    @Test
    public void testFormatNotRounded() {
        assertEquals("3,3", new HouseViewService().format(new BigDecimal("3.3"), false, false));
    }

    @Test
    public void testFormatRounded() {
        assertEquals("3,3", new HouseViewService().format(new BigDecimal("3.3"), true, false));
        assertEquals("3", new HouseViewService().format(new BigDecimal("3.0"), true, false));
        assertEquals("-0,3", new HouseViewService().format(new BigDecimal("-0.3"), true, false));
    }

    @Test
    public void testFormatRoundedAsInteger() {
        assertEquals("3", new HouseViewService().format(new BigDecimal("3.3"), true, true));
        assertEquals("3", new HouseViewService().format(new BigDecimal("3"), true, true));
        assertEquals("4", new HouseViewService().format(new BigDecimal("3.5"), true, true));
        assertEquals("0", new HouseViewService().format(new BigDecimal("-0.3"), true, true));
        assertEquals("-1", new HouseViewService().format(new BigDecimal("-0.6"), true, true));
    }
}