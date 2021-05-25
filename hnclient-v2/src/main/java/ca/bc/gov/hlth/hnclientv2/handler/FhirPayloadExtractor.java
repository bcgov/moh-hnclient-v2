package ca.bc.gov.hlth.hnclientv2.handler;

import java.io.UnsupportedEncodingException;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hnclientv2.error.CamelCustomException;
import ca.bc.gov.hlth.hncommon.json.fhir.FHIRJsonMessage;
import ca.bc.gov.hlth.hncommon.json.fhir.FHIRJsonUtil;
import ca.bc.gov.hlth.hncommon.util.LoggingUtil;
import ca.bc.gov.hlth.hncommon.util.StringUtil;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class FhirPayloadExtractor {

    private static final Logger logger = LoggerFactory.getLogger(FhirPayloadExtractor.class);
    private static final JSONParser jsonParser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);

    @Handler
    public static String extractFhirPayload(Exchange exchange,String fhirMessage) throws ParseException, UnsupportedEncodingException, CamelCustomException {

        JSONObject fhirMessageJSON = (JSONObject) jsonParser.parse(fhirMessage);
        
        FHIRJsonMessage encodedExtractedMessage = FHIRJsonUtil.parseJson2FHIRMsg(fhirMessageJSON); // get the data property

        // Only way to verify if message is base64 encoded is to decode and check for no exception
        // In case string is not Base 64, decoder throws IllegalArgumentException. Handled that exception.
        String extractedMessage;
        try {
        	extractedMessage = StringUtil.decodeBase64(encodedExtractedMessage.getV2MessageData());
        } catch(IllegalArgumentException e) {
        	logger.error("Exception while decoding message ", e);
        	throw new CamelCustomException(e.getMessage());
        }
		logger.debug("{}: The decoded HL7 message is: {}", LoggingUtil.getMethodName(), extractedMessage);
        
        return extractedMessage;
    }    
}
