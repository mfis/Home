package de.fimatas.home.library.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.fimatas.home.library.util.HomeUtils;

public class HomeUtilsTest {

	@Test
	public void testEscape() {
		assertThat(HomeUtils.escape("Änderung-Ende"), is("Aenderung_Ende"));
	}
	
}
