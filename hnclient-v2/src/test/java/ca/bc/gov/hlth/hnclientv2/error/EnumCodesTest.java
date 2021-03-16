package ca.bc.gov.hlth.hnclientv2.error;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;


import ca.bc.gov.hlth.hnclientv2.error.ErrorCodes;

public class EnumCodesTest {

	@Test
	public void testMessage_ErrorCode401() {

		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(401);

		assertEquals(retrieveEnumByValue, "Could not connect with the remote host");

	}

	@Test
	public void testMessage_ErrorCode500() {

		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(500);

		assertEquals(retrieveEnumByValue, "Error connecting to SERVER");

	}

	@Test
	public void testMessage_ErrorCode402() {

		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(402);

		assertEquals(retrieveEnumByValue, "Connection with remote facility timed out");

	}
	
	@Test
	public void testMessage_ErrorNull() {

		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(null);

		assertEquals(retrieveEnumByValue, "");

	}
	
	@Test(expected = NullPointerException.class)
	public void testMessage_ErrorCodeNotExist() {

		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(300);

		assertEquals(retrieveEnumByValue, "");
	}

	@Test
	public void testErrorCodes() {

		Map<Integer, ErrorCodes> errorCodeMap = ErrorCodes.errorCodeByErrorNumber;

		assertEquals(errorCodeMap.size(), 3);

    
	}

}
