package de.fimatas.home.controller.service;

import de.fimatas.home.library.domain.model.WeatherConditions;
import de.fimatas.home.library.domain.model.WeatherForecast;
import de.fimatas.home.library.domain.model.WeatherForecastConclusion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    @Test
    void testCalculateConclusionForTimerange_MinMax(){

        List<WeatherForecast> items = new LinkedList<>();
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 20, 0), new BigDecimal(20L), new BigDecimal(5L), new BigDecimal(6L), true, Set.of()));
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 21, 0), new BigDecimal(19L), new BigDecimal(4L), new BigDecimal(6L), false, Set.of()));

        final WeatherForecastConclusion conclusion = WeatherService.calculateConclusionForTimerange(items);

        assertEquals(new BigDecimal(19L), conclusion.getMinTemp());
        assertEquals(new BigDecimal(20L), conclusion.getMaxTemp());
        assertEquals(5, conclusion.getMaxWind());
    }

    @Test
    void testCalculateConclusionForTimerange_Snow(){

        List<WeatherForecast> items = new LinkedList<>();
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 20, 0), new BigDecimal(20L), new BigDecimal(5L), new BigDecimal(6L), true, Set.of(WeatherConditions.RAIN)));
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 21, 0), new BigDecimal(19L), new BigDecimal(4L), new BigDecimal(6L), false, Set.of(WeatherConditions.SNOW)));
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 22, 0), new BigDecimal(19L), new BigDecimal(4L), new BigDecimal(6L), false, Set.of(WeatherConditions.SNOW)));

        final WeatherForecastConclusion conclusion = WeatherService.calculateConclusionForTimerange(items);

        assertTrue(conclusion.getConditions().contains(WeatherConditions.SNOW));
        assertEquals(21, conclusion.getFirstOccurences().get(WeatherConditions.SNOW).getHour());
    }

    @Test
    void testCalculateConclusionForTimerange_KindOfRain_NotEnoughSun(){

        List<WeatherForecast> items = new LinkedList<>();
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 20, 0), new BigDecimal(20L), new BigDecimal(5L), new BigDecimal(6L), true, Set.of(WeatherConditions.SUN)));
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 21, 0), new BigDecimal(19L), new BigDecimal(4L), new BigDecimal(6L), false, Set.of(WeatherConditions.CLOUD_RAIN)));
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 22, 0), new BigDecimal(19L), new BigDecimal(4L), new BigDecimal(6L), false, Set.of(WeatherConditions.CLOUD_RAIN)));

        final WeatherForecastConclusion conclusion = WeatherService.calculateConclusionForTimerange(items);

        assertTrue(conclusion.getConditions().contains(WeatherConditions.CLOUD_RAIN));
        assertEquals(21, conclusion.getFirstOccurences().get(WeatherConditions.CLOUD_RAIN).getHour());
        assertFalse(conclusion.getConditions().contains(WeatherConditions.SUN));
    }

    @Test
    void testCalculateConclusionForTimerange_Sun(){

        List<WeatherForecast> items = new LinkedList<>();
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 20, 0), new BigDecimal(20L), new BigDecimal(5L), new BigDecimal(6L), true, Set.of(WeatherConditions.SUN)));
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 21, 0), new BigDecimal(19L), new BigDecimal(4L), new BigDecimal(6L), false, Set.of(WeatherConditions.SUN)));
        items.add(new WeatherForecast(LocalDateTime.of(2022, 5, 21, 22, 0), new BigDecimal(19L), new BigDecimal(4L), new BigDecimal(6L), false, Set.of(WeatherConditions.SUN)));

        final WeatherForecastConclusion conclusion = WeatherService.calculateConclusionForTimerange(items);

        assertTrue(conclusion.getConditions().contains(WeatherConditions.SUN));
        assertEquals(20, conclusion.getFirstOccurences().get(WeatherConditions.SUN).getHour());
    }
}