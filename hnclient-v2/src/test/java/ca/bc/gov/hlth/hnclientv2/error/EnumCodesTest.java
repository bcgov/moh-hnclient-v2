package ca.bc.gov.hlth.hnclientv2.error;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;


import ca.bc.gov.hlth.hnclientv2.error.ErrorCodes;

public class EnumCodesTest {


	@Test
	public void testMessage_ErrorCode400() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(400);
		assertEquals(retrieveEnumByValue, "Bad Request");
	}

	@Test
	public void testMessage_ErrorCode401() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(401);
		assertEquals(retrieveEnumByValue, "Unauthorized");
	}

	@Test
	public void testMessage_ErrorCode403() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(403);
		assertEquals(retrieveEnumByValue, "Forbidden");
	}

	@Test
	public void testMessage_ErrorCode404() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(404);
		assertEquals(retrieveEnumByValue, "Not Found");
	}

	@Test
	public void testMessage_ErrorCode405() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(405);
		assertEquals(retrieveEnumByValue, "Method Not Allowed");
	}

	@Test
	public void testMessage_ErrorCode408() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(408);
		assertEquals(retrieveEnumByValue, "Request Timeout");
	}
	
	@Test
	public void testMessage_ErrorCode500() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(500);
		assertEquals(retrieveEnumByValue, "Internal Server Error");
	}

	@Test
	public void testMessage_ErrorCode502() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(502);
		assertEquals(retrieveEnumByValue, "Bad Gateway");
	}

	@Test
	public void testMessage_ErrorCode503() {

		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(503);

		assertEquals(retrieveEnumByValue, "Service Unavailable");

	}
	
	@Test
	public void testMessage_ErrorCode504() {
		String retrieveEnumByValue = ErrorCodes.retrieveEnumByValue(504);
		assertEquals(retrieveEnumByValue, "Gateway Timeout");
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
		assertEquals(errorCodeMap.size(), 10);
	}

}
