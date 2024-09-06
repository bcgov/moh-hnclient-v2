package ca.bc.gov.hlth.hnclientv2.handler;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hnclientv2.error.CamelCustomException;
import ca.bc.gov.hlth.hnclientv2.error.ErrorBuilder;
import ca.bc.gov.hlth.hnclientv2.error.MessageUtil;
import ca.bc.gov.hlth.hncommon.json.fhir.FHIRJsonMessage;
import ca.bc.gov.hlth.hncommon.json.fhir.FHIRJsonUtil;
import ca.bc.gov.hlth.hncommon.util.LoggingUtil;
import ca.bc.gov.hlth.hncommon.util.StringUtil;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class FhirPayloadExtractor {

    private static final Logger logger = LoggerFactory.getLogger(FhirPayloadExtractor.class);

    private boolean preserveMessage;
    
	public FhirPayloadExtractor() {
	}

	public FhirPayloadExtractor(boolean preserveMessage) {
		this.preserveMessage = preserveMessage;
	}

    @Handler
    public String extractFhirPayload(Exchange exchange, String fhirMessage) throws CamelCustomException {
    	// Assume that an empty response body means there is an issue with the server
    	// and return an error response
    	if (StringUtils.isBlank(fhirMessage)) {
    		logger.error("{} - Response from HNS ESB has empty body", LoggingUtil.getMethodName());
    		return ErrorBuilder.buildErrorMessage(MessageUtil.SERVER_NO_CONNECTION);
    	}

        JSONObject fhirMessageJSON;
		try {
	    	JSONParser jsonParser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			fhirMessageJSON = (JSONObject) jsonParser.parse(fhirMessage);
		} catch (Exception e) {
			// Assume that a parsing exception means there is an issue with the server 
	    	// and return an error response.
			// E.g. a 404 error will likely return HTML
			logger.error("{} - Response from HNS ESB has invalid body: {}", LoggingUtil.getMethodName(), e.getMessage());
    		return ErrorBuilder.buildErrorMessage(MessageUtil.SERVER_NO_CONNECTION);
		}
        
        FHIRJsonMessage encodedExtractedMessage = FHIRJsonUtil.parseJson2FHIRMsg(fhirMessageJSON); // get the data property

        // Only way to verify if message is base64 encoded is to decode and check for no exception
        // In case string is not Base 64, decoder throws IllegalArgumentException. Handled that exception.
        String extractedMessage;
        
        try {
        	/* Some connected partners applications that consume the response message required that any carriage return
        	 * in the message be kept. For these cases the message is not trimmed as this removes additional characters including 
        	 * carriage returns. The standard behaviour is to trim the message.
        	 *   
        	 */
        	if (preserveMessage) {
        		extractedMessage =  StringUtil.decodeBase64(encodedExtractedMessage.getV2MessageData());
        	} else {
        		extractedMessage =  StringUtils.trim(StringUtil.decodeBase64(encodedExtractedMessage.getV2MessageData()));
        	}
        } catch (IllegalArgumentException e) {
        	logger.error("{} - Exception while decoding message: {}", LoggingUtil.getMethodName(), e.getMessage());
        	throw new CamelCustomException(e.getMessage());
        }
		logger.debug("{}: The decoded HL7 message is:\n{}", LoggingUtil.getMethodName(), extractedMessage);
		logger.debug("{}: End Message.", LoggingUtil.getMethodName());
		
        return extractedMessage;
    }    
	
}
