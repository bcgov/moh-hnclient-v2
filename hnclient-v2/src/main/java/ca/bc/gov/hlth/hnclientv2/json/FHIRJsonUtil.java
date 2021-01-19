/**
 * 
 */
package ca.bc.gov.hlth.hnclientv2.json;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.StringUtil;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * @author Tony.Ma
 * @date Jan. 15, 2021
 *
 */
public final class FHIRJsonUtil {

	private static final Logger logger = LoggerFactory.getLogger(FHIRJsonUtil.class);
	
	public static String FHIR_JSONMESSAGE_TYPE = "contentType";
	public static String FHIR_JSONMESSAGE_DATA = "data";
	public static String FHIR_JSONMESSAGE_ATTACHMENT = "attachment";
	public static String FHIR_JSONMESSAGE_RESOURCETYPE = "resourceType";
	public static String FHIR_JSONMESSAGE_STATUS = "status";
	public static String FHIR_JSONMESSAGE_CONTENT = "content";
	
	/**
	 * This method is used to create a Json Object for the BC health V2 HL7 message.
	 * 
	 * A null should be returned if the HL7 message is empty.
	 * 
	 * @param hl7
	 * @return JSONObject
	 */
	
	public static JSONObject createFHIRJsonObj(final String hl7) {

		// if the content of HL7 message is null/empty, return null;
		if (StringUtil.isNullOrEmpty(hl7))
			return null;

		// init a JSON object
		JSONObject v2JsonObj = new JSONObject();
		JSONArray contentArrary = new JSONArray();
		JSONObject contentObj = new JSONObject();
		JSONObject attachmentObj = new JSONObject();

		// There is a FHIR message specification of the BC Health. This piece of code is
		// based on this specification. Please refer to the
		// https://github.com/bcgov/bcmoh-iam-integration-guide/wiki/FHIR-message-specification-to-wrap-HL7v2-messages
		// for details.
		
		attachmentObj.put(FHIR_JSONMESSAGE_TYPE, "x-application/hl7-v2+er7");
		attachmentObj.put(FHIR_JSONMESSAGE_DATA, hl7);
		contentObj.put(FHIR_JSONMESSAGE_ATTACHMENT, attachmentObj);
		contentArrary.add(contentObj);
		v2JsonObj.put(FHIR_JSONMESSAGE_RESOURCETYPE, "DocumentReference");
		v2JsonObj.put(FHIR_JSONMESSAGE_STATUS, "current");
		v2JsonObj.put(FHIR_JSONMESSAGE_CONTENT, contentArrary);
		logger.debug("The JSON Message is:"+v2JsonObj.toJSONString());
		//return the JSON object
		return v2JsonObj; 
		
	}	
	
	/**
	 * This method is for parse a JSON message into a FHIR Message object based on the specification
	 * 
	 * @param jsonObj
	 * @return	FHIRJsonMessage
	 */
	public static FHIRJsonMessage parseJson2FHIRMsg(final JSONObject jsonObj) {
		
		FHIRJsonMessage fhirJsonMsg = new FHIRJsonMessage();
		
		if (jsonObj == null) return null;		

		for (Entry<String, Object> hs : jsonObj.entrySet()) {
			String key = hs.getKey();
			Object value = hs.getValue();
			if (value instanceof String) {
				if (key.equals(FHIR_JSONMESSAGE_RESOURCETYPE))
					fhirJsonMsg.setRecourceType(value.toString());
				else if (key.equals(FHIR_JSONMESSAGE_STATUS))
					fhirJsonMsg.setStatus(value.toString());
				else {
					logger.error("This is not an valid FHIR message!");
					return null;
				}
			}
			if (key.equals(FHIR_JSONMESSAGE_CONTENT) && value instanceof JSONArray) {
				JSONObject contentJson = (JSONObject) ((JSONArray) value).get(0);
				JSONObject attachmentJson = (JSONObject) contentJson.get(FHIR_JSONMESSAGE_ATTACHMENT);

				for (Entry<String, Object> attach : attachmentJson.entrySet()) {
					String attachKey = attach.getKey();
					Object attachValue = attach.getValue();
					if (attachKey.equals(FHIR_JSONMESSAGE_TYPE))
						fhirJsonMsg.setContentType(attachValue.toString());
					else if (attachKey.equals(FHIR_JSONMESSAGE_DATA))
						fhirJsonMsg.setV2MessageData(attachValue.toString());
					else {
						logger.error("This is not an valid FHIR message!");
						return null;
					}
				}

			}
		}
		return fhirJsonMsg;		
	}	
}
