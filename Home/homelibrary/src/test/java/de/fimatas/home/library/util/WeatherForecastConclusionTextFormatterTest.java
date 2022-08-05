package de.fimatas.home.library.util;

import de.fimatas.home.library.domain.model.WeatherConditions;
import de.fimatas.home.library.domain.model.WeatherForecastConclusion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter.*;
import static org.junit.jupiter.api.Assertions.*;

class WeatherForecastConclusionTextFormatterTest {

    @Test
    void testFormatConclusionText_NoConditions(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setConditions(List.of());

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("", map.get(FORMAT_CONDITIONS_1_MAX));
        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_OneConditionWind(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(25);
        conclusion.setConditions(List.of(WeatherConditions.WIND));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Wind 25 km/h", map.get(FORMAT_CONDITIONS_1_MAX));
        assertEquals("18..23°C, Wind 25 km/h", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Wind 25 km/h", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Wind bis 25 km/h", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_OneConditionOtherThenWind(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setConditions(List.of(WeatherConditions.SUN));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Sonne", map.get(FORMAT_CONDITIONS_1_MAX));
        assertEquals("18..23°C, Sonne", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Sonne", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Sonne", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_TwoConditionsPlusOneUnsignificant(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setConditions(List.of(WeatherConditions.SUN, WeatherConditions.RAIN, WeatherConditions.SUN_CLOUD));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Regen +", map.get(FORMAT_CONDITIONS_1_MAX));
        assertEquals("18..23°C, Regen +", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Sonne, Regen", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Sonne, Regen", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_UnsignificantConditions(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setConditions(List.of(WeatherConditions.SUN_CLOUD));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("", map.get(FORMAT_CONDITIONS_1_MAX));
        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Leicht bewölkt", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Leicht bewölkt", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_WithTime(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setConditions(List.of(WeatherConditions.SUN, WeatherConditions.RAIN));
        conclusion.getFirstOccurences().put(WeatherConditions.SUN, LocalDateTime.of(2022,5,23,15,0));
        conclusion.getFirstOccurences().put(WeatherConditions.RAIN, LocalDateTime.of(2022,5,23,19,0));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("Temperatur 18 bis 23°C, Sonne ab 15 Uhr, Regen ab 19 Uhr", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_SameMinMaxs(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(18L));
        conclusion.setMaxWind(10);
        conclusion.setConditions(List.of());

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("", map.get(FORMAT_CONDITIONS_1_MAX));
        assertEquals("18°C", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18°C", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18°C", map.get(FORMAT_LONGEST));
    }
}