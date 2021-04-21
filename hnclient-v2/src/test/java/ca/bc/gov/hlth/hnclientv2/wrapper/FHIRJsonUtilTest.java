/**
 * 
 */
package ca.bc.gov.hlth.hnclientv2.wrapper;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * @author Tony.Ma * 
 * @date Jan. 19, 2021
 *
 */
public class FHIRJsonUtilTest {
	
	public String msgE45 = "MSH|^~\\&|HR|BC00000098|RAIGET-DOC-SUM|BC0003000|19991004103039|lharris|E45|19980915000015|D|2.3\r\n" + 
			"HDR|||TRAININGAdmin\r\n" + 
			"SFT|1.0||testorg^^orgid^^^MOH|1.0|barebones||\r\n" + 
			"QPD|E45^^HNET0003|1|^^00000001^^^CANBC^XX^MOH|^^00000001^^^CANBC^XX^MOH|^^00000754^^^CANBC^XX^MOH|9020198746^^^CANBC^JHN^MOH||19421112||||||19980601||PVC^^HNET9909||\r\n" + 
			"RCP|I|";
	
	public String msgr30="MSH|^~\\&|HR|BC00000098|RAIEST-PYR-RL|BC0003000|19990719125635|PM33283|R30|19980915000017|D|2.3\r\n" + 
			"ZHD|19990719125635|^^00000001|TRAININGAdmin\r\n" + 
			"PID||9081361205^^^BC^PH\r\n" + 
			"IN1||||||||3131182||123|^^1234|20010601\r\n" + 
			"ZIA||||||||||||||||123 FIRST ST^TOWN BC^^^^^^^^^^^^^^^^^^^^V9A1H9^^H|^PH^PH^^^123^1235555~^PH^PH^^^999^1115555\r\n" + 
			"NK1|||SP||||||||||||||||||||||||||||||1233094356^^^BC^PH\r\n" +
			"NK1|||DP||||||||||||||||||||||||||||||1236913098^^^BC^PH\r\n" +
			"NK1|||DP||||||||||||||||||||||||||||||1233410453^^^BC^PH";
	
	public String msgR31="MSH|^~\\&|HR|BC00000098|RAIEST_PYR-RLDP|BC0003000|19991013153348|PM33283|R31|19980915000015|D|2.3\r\n" + 
			"ZHD|19991013153348|^^00000001|TRAININGAdmin\r\n" + 
			"PID||1234790865^^^BC345^PH|12345678901234567890^^^BC345^MH\r\n" +
			"IN1||||||||1250000||||20010101\r\n" + 
			"NK1|||12||||||||||||||||||||||||||||||1234567890^^^12345^12\r\n" + 
			"ZSG|||||";
	
	@Test
	public void testCreateFHIRJsonNullObject() {		
		JSONObject jsonObj = FHIRJsonUtil.createFHIRJsonObj(null);
		assertNull(jsonObj);
	}

	@Test
	public void testCreateFHIRJsonEmptyObject() {
		JSONObject jsonObj = FHIRJsonUtil.createFHIRJsonObj("");
		assertNull(jsonObj);
	}

	@Test
	public void testDoNotThrowCreateFHIRJsonObj_msgE45()  {
		assertDoesNotThrow(() -> FHIRJsonUtil.createFHIRJsonObj(msgE45));
	}
	
	@Test
	public void testIsJSONMsg()  {
		JSONObject fhirObj = FHIRJsonUtil.createFHIRJsonObj(msgE45);
		assertTrue(JSONValue.isValidJson(fhirObj.toJSONString()));
	}
		
	@Test
	public void testCreateFHIRJsonObj_msgE45()  {	
		JSONObject fhirObj = FHIRJsonUtil.createFHIRJsonObj(msgE45);
		String expectedJsonString = "{\"content\":[{\"attachment\":{\"data\":\"MSH|^~\\\\&|HR|BC00000098|RAIGET-DOC-SUM|BC0003000|19991004103039|lharris|E45|19980915000015|D|2.3\\r\\nHDR|||TRAININGAdmin\\r\\nSFT|1.0||testorg^^orgid^^^MOH|1.0|barebones||\\r\\nQPD|E45^^HNET0003|1|^^00000001^^^CANBC^XX^MOH|^^00000001^^^CANBC^XX^MOH|^^00000754^^^CANBC^XX^MOH|9020198746^^^CANBC^JHN^MOH||19421112||||||19980601||PVC^^HNET9909||\\r\\nRCP|I|\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";
		String actualString = fhirObj.toJSONString();
		assertEquals(expectedJsonString,actualString);
	}
	
	@Test
	public void testCreateFHIRJsonObj_msgr30() {
		JSONObject fhirObj = FHIRJsonUtil.createFHIRJsonObj(msgr30);
		String expectedJsonString = "{\"content\":[{\"attachment\":{\"data\":\"MSH|^~\\\\&|HR|BC00000098|RAIEST-PYR-RL|BC0003000|19990719125635|PM33283|R30|19980915000017|D|2.3\\r\\nZHD|19990719125635|^^00000001|TRAININGAdmin\\r\\nPID||9081361205^^^BC^PH\\r\\nIN1||||||||3131182||123|^^1234|20010601\\r\\nZIA||||||||||||||||123 FIRST ST^TOWN BC^^^^^^^^^^^^^^^^^^^^V9A1H9^^H|^PH^PH^^^123^1235555~^PH^PH^^^999^1115555\\r\\nNK1|||SP||||||||||||||||||||||||||||||1233094356^^^BC^PH\\r\\nNK1|||DP||||||||||||||||||||||||||||||1236913098^^^BC^PH\\r\\nNK1|||DP||||||||||||||||||||||||||||||1233410453^^^BC^PH\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";
		String actualString = fhirObj.toJSONString();
		assertEquals(expectedJsonString,actualString);
	}
	
	@Test
	public void testParseFHIRJsonObj_msgE45()  {
		FHIRJsonMessage fhirJson = FHIRJsonUtil.parseJson2FHIRMsg(FHIRJsonUtil.createFHIRJsonObj(msgE45));
		String data = fhirJson.getV2MessageData();
		assertEquals(fhirJson.getContentType(),"x-application/hl7-v2+er7");
		assertEquals(fhirJson.getResourceType(),"DocumentReference");
		assertEquals(fhirJson.getStatus(),"current");
	    assertEquals(data,msgE45);
	}
	
	@Test
	public void testParseFHIRJsonObj_msgr30()  {
		FHIRJsonMessage fhirJson = FHIRJsonUtil.parseJson2FHIRMsg(FHIRJsonUtil.createFHIRJsonObj(msgr30));
		String data = fhirJson.getV2MessageData();
		assertEquals(fhirJson.getContentType(),"x-application/hl7-v2+er7");
		assertEquals(fhirJson.getResourceType(),"DocumentReference");
		assertEquals(fhirJson.getStatus(),"current");
	    assertEquals(data,msgr30);
	}

	@Test
	public void testParseFHIRJsonObj_msgR31()  {
		FHIRJsonMessage fhirJson = FHIRJsonUtil.parseJson2FHIRMsg(FHIRJsonUtil.createFHIRJsonObj(msgR31));
		String data = fhirJson.getV2MessageData();
		assertEquals(fhirJson.getContentType(),"x-application/hl7-v2+er7");
		assertEquals(fhirJson.getResourceType(),"DocumentReference");
		assertEquals(fhirJson.getStatus(),"current");
	    assertEquals(data,msgR31);
	}
}
