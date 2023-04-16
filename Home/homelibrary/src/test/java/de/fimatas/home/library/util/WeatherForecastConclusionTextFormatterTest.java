package de.fimatas.home.library.util;

import de.fimatas.home.library.domain.model.WeatherConditions;
import de.fimatas.home.library.domain.model.WeatherForecastConclusion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static de.fimatas.home.library.util.WeatherForecastConclusionTextFormatter.*;
import static org.junit.jupiter.api.Assertions.*;

class WeatherForecastConclusionTextFormatterTest {

    @Test
    void testFormatConclusionText_NoConditions(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setMaxGust(20);
        conclusion.setConditions(Set.of());

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
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
        conclusion.setMaxGust(30);
        conclusion.setConditions(Set.of(WeatherConditions.WIND));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Wind", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
        assertEquals("18..23°C, Wind 25..30 km/h", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Wind 25..30 km/h", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Wind 25..30 km/h", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_OneConditionGust(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(25);
        conclusion.setMaxGust(80);
        conclusion.setConditions(Set.of(WeatherConditions.GUST));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Böen", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
        assertEquals("18..23°C, Böen 25..80 km/h", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Böen 25..80 km/h", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Böen 25..80 km/h", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_OneConditionWindSameGustValue(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(25);
        conclusion.setMaxGust(25);
        conclusion.setConditions(Set.of(WeatherConditions.WIND, WeatherConditions.MOON));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Wind", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
        assertEquals("18..23°C, Wind bis 25 km/h", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Wind bis 25 km/h", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Wind bis 25 km/h", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_OneConditionOtherThenWind(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setMaxGust(20);
        conclusion.setConditions(Set.of(WeatherConditions.SUN));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Sonne", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
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
        conclusion.setMaxGust(20);
        conclusion.setConditions(Set.of(WeatherConditions.RAIN, WeatherConditions.SUN, WeatherConditions.SUN_CLOUD));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Regen +", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
        assertEquals("18..23°C, Regen +", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Regen, Sonne, Leicht bewölkt", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Regen, Sonne, Leicht bewölkt", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_TwoSignificantConditions(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(-5L));
        conclusion.setMaxTemp(new BigDecimal(5L));
        conclusion.setMaxWind(10);
        conclusion.setMaxGust(20);
        conclusion.setConditions(Set.of(WeatherConditions.RAIN, WeatherConditions.SNOW));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("Schnee +", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
        assertEquals("-5..5°C, Schnee +", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
    }

    @Test
    void testFormatConclusionText_UnsignificantConditions(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setMaxGust(20);
        conclusion.setConditions(Set.of(WeatherConditions.SUN_CLOUD, WeatherConditions.MOON));

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18..23°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("Leicht bewölkt", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
        assertEquals("Leicht bewölkt", map.get(FORMAT_CONDITIONS_SHORT_1_MAX_INCL_UNSIGNIFICANT));
        assertEquals("18..23°C, Leicht bewölkt", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18 bis 23°C, Leicht bewölkt", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18 bis 23°C, Leicht bewölkt", map.get(FORMAT_LONGEST));
    }

    @Test
    void testFormatConclusionText_WithTime(){

        final var conclusion = new WeatherForecastConclusion();
        conclusion.setMinTemp(new BigDecimal(18L));
        conclusion.setMaxTemp(new BigDecimal(23L));
        conclusion.setMaxWind(10);
        conclusion.setMaxGust(20);
        conclusion.setConditions(Set.of(WeatherConditions.SUN, WeatherConditions.RAIN));
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
        conclusion.setMaxGust(20);
        conclusion.setConditions(Set.of());

        final Map<Integer, String> map = WeatherForecastConclusionTextFormatter.formatConclusionText(conclusion);

        assertEquals("18°C", map.get(FORMAT_FROM_TO_ONLY));
        assertEquals("", map.get(FORMAT_CONDITIONS_SHORT_1_MAX));
        assertEquals("18°C", map.get(FORMAT_FROM_TO_PLUS_1_MAX));
        assertEquals("18°C", map.get(FORMAT_FROM_TO_ALL_SIGNIFICANT_CONDITIONS));
        assertEquals("Temperatur 18°C", map.get(FORMAT_LONGEST));
    }
}