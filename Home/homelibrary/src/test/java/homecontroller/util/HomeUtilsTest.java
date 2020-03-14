package homecontroller.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HomeUtilsTest {

	@Test
	public void testEscape() {
		assertThat(HomeUtils.escape("Änderung-Ende"), is("Aenderung_Ende"));
	}
	
}
