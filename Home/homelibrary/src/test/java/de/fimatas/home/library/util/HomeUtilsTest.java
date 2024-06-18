package de.fimatas.home.library.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

import java.math.BigDecimal;

public class HomeUtilsTest {

    @Test
    public void testEscape() {
        assertThat(HomeUtils.escape("Änderung-Ende"), is("Aenderung_Ende"));
    }

    @Test
    public void testRoundPercentageToNearestTen(){
        assertThat(HomeUtils.roundPercentageToNearestTen(0), is(0));
        assertThat(HomeUtils.roundPercentageToNearestTen(4), is(0));
        assertThat(HomeUtils.roundPercentageToNearestTen(5), is(10));
        assertThat(HomeUtils.roundPercentageToNearestTen(9), is(10));
        assertThat(HomeUtils.roundPercentageToNearestTen(10), is(10));
        assertThat(HomeUtils.roundPercentageToNearestTen(14), is(10));
        assertThat(HomeUtils.roundPercentageToNearestTen(20), is(20));
        assertThat(HomeUtils.roundPercentageToNearestTen(22), is(20));
        assertThat(HomeUtils.roundPercentageToNearestTen(91), is(90));
        assertThat(HomeUtils.roundPercentageToNearestTen(95), is(100));
        assertThat(HomeUtils.roundPercentageToNearestTen(100), is(100));
    }

    @Test
    public void testRoundAndFormatPrecipitation(){
        assertThat(HomeUtils.roundAndFormatPrecipitation(new BigDecimal("0.2")), is("<1mm"));
        assertThat(HomeUtils.roundAndFormatPrecipitation(new BigDecimal("0.5")), is("1mm"));
        assertThat(HomeUtils.roundAndFormatPrecipitation(new BigDecimal("1.4")), is("1mm"));
        assertThat(HomeUtils.roundAndFormatPrecipitation(new BigDecimal("1.7")), is("2mm"));
    }
}
