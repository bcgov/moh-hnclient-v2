package ca.bc.gov.hlth.hnclientv2;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit test class for Util
 *
 */
public class UtilTest {

	public static String nullValue = null;
	public static String emptyValue = "";
	
	@Test
	public void testNull_RequireNonBlank(){
		assertThrows(NullPointerException.class, () -> {
			Util.requireNonBlank(nullValue,"This is an expected exception!!" );
		});
	}
	
	@Test
	public void testEmpty_RequireNonBlank(){
		assertThrows(NullPointerException.class, () -> {
			Util.requireNonBlank(emptyValue,"This is an expected exception!!" );
		});
	}
}
