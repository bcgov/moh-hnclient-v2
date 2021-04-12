package ca.bc.gov.hlth.hnclientv2;

import org.junit.Test;

/**
 * @author Tony.Ma * 
 * @date Jan. 21, 2021
 *
 */
public class UtilTest {

	public static String nullValue = null;
	public static String emptyValue = "";
	
	@Test(expected = NullPointerException.class)
	public void testNull_RequireNonBlank(){
		Util.requireNonBlank(nullValue,"This is an expected exception!!" );
	}
	
	@Test(expected = NullPointerException.class)
	public void testEmpty_RequireNonBlank(){
		Util.requireNonBlank(emptyValue,"This is an expected exception!!" );
	}
}
