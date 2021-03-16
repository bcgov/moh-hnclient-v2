package ca.bc.gov.hlth.hnclientv2.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.UnsupportedEncodingException;

import ca.bc.gov.hlth.hnclientv2.Util;
import org.junit.Test;

/**
 * @author Tony.Ma * 
 * @date Jan. 21, 2021
 *
 */
public class UtilTest {

	public static String msgE45 = "MSH|^~\\&|HR|BC00000098|RAIGET-DOC-SUM|BC0003000|19991004103039|lharris|E45|19980915000015|D|2.3"
			+ "HDR|||TRAININGAdmin\r\n" + "SFT|1.0||testorg^^orgid^^^MOH|1.0|barebones||\r\n"
			+ "QPD|E45^^HNET0003|1|^^00000001^^^CANBC^XX^MOH|^^00000001^^^CANBC^XX^MOH|^^00000754^^^CANBC^XX^MOH|9020198746^^^CANBC^JHN^MOH||19421112||||||19980601||PVC^^HNET9909||\r\n"
			+ "RCP|I|";

	public static String encodeMsg45 = "TVNIfF5+XCZ8SFJ8QkMwMDAwMDA5OHxSQUlHRVQtRE9DLVNVTXxCQzAwMDMwMDB8MTk5OTEwMDQxMDMwMzl8bGhhcnJpc3xFNDV8MTk5ODA5MTUwMDAwMTV8RHwyLjNIRFJ8fHxUUkFJTklOR0FkbWluDQpTRlR8MS4wfHx0ZXN0b3JnXl5vcmdpZF5eXk1PSHwxLjB8YmFyZWJvbmVzfHwNClFQRHxFNDVeXkhORVQwMDAzfDF8Xl4wMDAwMDAwMV5eXkNBTkJDXlhYXk1PSHxeXjAwMDAwMDAxXl5eQ0FOQkNeWFheTU9IfF5eMDAwMDA3NTReXl5DQU5CQ15YWF5NT0h8OTAyMDE5ODc0Nl5eXkNBTkJDXkpITl5NT0h8fDE5NDIxMTEyfHx8fHx8MTk5ODA2MDF8fFBWQ15eSE5FVDk5MDl8fA0KUkNQfEl8";
	
	public static String invalidBase64Charaters = "yk===klsdfklk";
	
	public static String nullValue = null;
	public static String emptyValue = "";
	
	@Test(expected = IllegalArgumentException.class)
	public void testNull_RequireNonBlank(){
		Util.requireNonBlank(nullValue,"This is an expected exception!!" );
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEmpty_RequireNonBlank(){
		Util.requireNonBlank(emptyValue,"This is an expected exception!!" );
	}
	
	@Test
	public void testNullEncodeBase64() {
		assertNull(Util.encodeBase64(nullValue));
	}

	@Test
	public void testEncodeBase64() {
		String expectedValue = encodeMsg45;
		String actualValue = Util.encodeBase64(msgE45);
		assertEquals(expectedValue, actualValue);
	}

	@Test
	public void testNullDecodeBase64() throws UnsupportedEncodingException {
		assertNull(Util.decodeBase64(nullValue));
	}

	@Test
	public void testDecodeBase64() throws UnsupportedEncodingException {
		String expectedValue = msgE45;
		String	actualValue = Util.decodeBase64(encodeMsg45);
		assertEquals(expectedValue, actualValue);
	}
	
	@Test
	public void testBothEncode_Decode64() throws UnsupportedEncodingException{
		String expectedValue = msgE45;
		String	actualValue = Util.decodeBase64(Util.encodeBase64(msgE45));
		assertEquals(expectedValue, actualValue);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testDecodeException() throws UnsupportedEncodingException{
		Util.decodeBase64(invalidBase64Charaters);
	}
}
