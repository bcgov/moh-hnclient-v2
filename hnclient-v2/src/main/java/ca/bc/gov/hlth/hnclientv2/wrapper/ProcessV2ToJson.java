package ca.bc.gov.hlth.hnclientv2.wrapper;

import org.apache.camel.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hnclientv2.error.NoInputHL7Exception;
import io.netty.util.internal.StringUtil;

public class ProcessV2ToJson {
	
	private static final Logger logger = LoggerFactory.getLogger(ProcessV2ToJson.class);
	
	@Handler
	public String convertToFHIRJson(String base64V2Message) throws NoInputHL7Exception {
		logger.debug("process: Trying to create a JSON Message {}", base64V2Message);

		// It should be impossible for the body to be empty here (the handshake server should catch that) but handle it just in case
		if (StringUtil.isNullOrEmpty(base64V2Message)) {
			throw new NoInputHL7Exception();
		}
		return FHIRJsonUtil.createFHIRJsonObj(base64V2Message).toString();
	}
}
