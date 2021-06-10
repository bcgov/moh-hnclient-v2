package ca.bc.gov.hlth.hnclientv2.error;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ErrorBuilderUnitTest {
	private static String v2Msg= "MSH|~\\&|HNWEB|moh_hnclient_dev|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|9826|D|2.4||\r\n"
			+ "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n" + "PID||1234567890^^^BC^PH";
	

	@Test
	void test_buildErrorMessage_whenV2MsgAndErrMsgIsNull() {
		
		String errMsg =ErrorBuilder.buildErrorMessage(null, null);
		String dataSegments[] = errMsg.split("\n");
		String expectedMSH ="MSH|^~\\\\&|HNCLIENT|UNKNOWNFACILITY|HNCLIENT|UNKNOWNCLIENT";
		String expectedMSA = "MSA|AR|||||";	
		assertEquals(dataSegments[0].substring(0,57), expectedMSH);
		assertEquals(dataSegments[1],expectedMSA);
	}
	
	@Test
	void test_buildErrorMessage_whenV2MsgIsNull() {
		
		String errMsg =ErrorBuilder.buildErrorMessage(null, "HNET_RTRN_INVALIDFORMATERROR");
		String dataSegments[] = errMsg.split("\n");
		String expectedMSH ="MSH|^~\\\\&|HNCLIENT|UNKNOWNFACILITY|HNCLIENT|UNKNOWNCLIENT";
		String expectedMSA = "MSA|AR||HNET_RTRN_INVALIDFORMATERROR|||";	
		assertEquals(dataSegments[0].substring(0,57), expectedMSH);
		assertEquals(dataSegments[1],expectedMSA);
	}
	
	@Test
	void test_buildErrorMessage() {
		String errMsg =ErrorBuilder.buildErrorMessage(v2Msg, "HNET_RTRN_INVALIDFORMATERROR");
		String dataSegments[] = errMsg.split("\n");
		String expectedMSH ="MSH|~\\&|HNCLIENT|BC00001013|HNCLIENT|moh_hnclient_dev";
		String expectedMSHMSg ="train96|ACK|R03|9826|D|2.4";
		String expectedMSA = "MSA|AR|9826|HNET_RTRN_INVALIDFORMATERROR|||";	
		assertEquals(dataSegments[0].substring(0,53), expectedMSH);
		assertEquals(dataSegments[0].substring(78), expectedMSHMSg);
		assertEquals(dataSegments[1],expectedMSA);
	}

}
