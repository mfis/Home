package de.fimatas.home.controller.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HumidityCalculatorTest {

    private HumidityCalculator humidityCalculator = new HumidityCalculator();

    private static final double DELTA = 0.01d;

    @Test
    public void testSimpleReloAbs() {
        assertEquals(8.641, humidityCalculator.relToAbs(20, 50), DELTA);
        assertEquals(3.758, humidityCalculator.relToAbs(10, 40), DELTA);
        assertEquals(0.942, humidityCalculator.relToAbs(-10, 40), DELTA);
    }

    @Test
    public void testLowerTempHigherRelHumidity() {
        double hR1 = 50;
        double hA1 = humidityCalculator.relToAbs(20, hR1);
        double hR2 = humidityCalculator.absToRel(15, hA1);
        assertTrue(hR2 > hR1);
    }

    @Test
    public void testExtremValuesTwoDirections() {
        for (int t = -50; t <= 100; t++) {
            for (int h = 0; h <= 100; h++) {
                double abs = humidityCalculator.relToAbs(t, h);
                double rel = humidityCalculator.absToRel(t, abs);
                // System.out.println(h + " " + rel);
                assertEquals(h, rel, DELTA);
            }
        }

    }

}
