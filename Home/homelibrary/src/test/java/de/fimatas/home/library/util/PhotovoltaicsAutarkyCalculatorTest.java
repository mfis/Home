package de.fimatas.home.library.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ConstantValue")
public class PhotovoltaicsAutarkyCalculatorTest {

    @Test
    void testAutarkyWithBothZero() {
        BigDecimal consumption = BigDecimal.ZERO;
        BigDecimal gridElectricity = BigDecimal.ZERO;
        int autarky = PhotovoltaicsAutarkyCalculator.calculateAutarkyPercentage(consumption, gridElectricity);
        assertEquals(100, autarky);
    }

    @Test
    void testAutarkyWithBothNull() {
        BigDecimal consumption = null;
        BigDecimal gridElectricity = null;
        int autarky = PhotovoltaicsAutarkyCalculator.calculateAutarkyPercentage(consumption, gridElectricity);
        assertEquals(100, autarky);
    }

    @Test
    void testAutarkyWithNullConsumption() {
        BigDecimal consumption = null;
        BigDecimal gridElectricity = new BigDecimal("1000.0");
        int autarky = PhotovoltaicsAutarkyCalculator.calculateAutarkyPercentage(consumption, gridElectricity);
        assertEquals(0, autarky);
    }

    @Test
    void testAutarkyWithNullGridElectricity() {
        BigDecimal consumption = new BigDecimal("1000.0");
        BigDecimal gridElectricity = null;
        int autarky = PhotovoltaicsAutarkyCalculator.calculateAutarkyPercentage(consumption, gridElectricity);
        assertEquals(100, autarky);
    }


    @Test
    void testAutarkyWithValidConsumption() {
        BigDecimal consumption = new BigDecimal("1500.0");
        BigDecimal gridElectricity = new BigDecimal("500.0");
        int autarky = PhotovoltaicsAutarkyCalculator.calculateAutarkyPercentage(consumption, gridElectricity);
        assertEquals(67, autarky);
    }

    @Test
    void testAutarkyWithEqualConsumptionAndGridElectricity() {
        BigDecimal consumption = new BigDecimal("1000.0");
        BigDecimal gridElectricity = new BigDecimal("1000.0");
        int autarky = PhotovoltaicsAutarkyCalculator.calculateAutarkyPercentage(consumption, gridElectricity);
        assertEquals(0, autarky);
    }

    @Test
    void testAutarkyWithMoreGridElectricityThanConsumption() {
        BigDecimal consumption = new BigDecimal("800.0");
        BigDecimal gridElectricity = new BigDecimal("1000.0");
        int autarky = PhotovoltaicsAutarkyCalculator.calculateAutarkyPercentage(consumption, gridElectricity);
        assertEquals(0, autarky);
    }
}
