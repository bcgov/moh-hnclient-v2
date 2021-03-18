package ca.bc.gov.hlth.hnclientv2.wrapper;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.StringUtil;

public class ProcessV2ToJson implements Processor {
	
	private static final Logger logger = LoggerFactory.getLogger(ProcessV2ToJson.class);
	
	@Override
	public void process(Exchange exchange) {
		logger.debug("Trying to create a JSON Message.");

		Object exchangeBody = exchange.getIn().getBody();

		// TODO it should be impossible for the body to be empty here (the handshake server or base64 encoder should catch that)
		// if we keep this, then we should throw an exception that causes an HL7Error_Msg_NoInputHL7 response if it is empty
		if (exchangeBody == null || StringUtil.isNullOrEmpty(exchangeBody.toString())) {
			throw new IllegalArgumentException("v2Message can't be null or empty");
		} else {
			String message = exchangeBody.toString();
			exchange.getIn().setBody(FHIRJsonUtil.createFHIRJsonObj(message).toString());
		}
	}
}
