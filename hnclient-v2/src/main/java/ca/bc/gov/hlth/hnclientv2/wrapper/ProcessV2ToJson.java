package ca.bc.gov.hlth.hnclientv2.wrapper;

import java.util.Objects;

import ca.bc.gov.hlth.hnclientv2.json.FHIRJsonUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.StringUtil;

/**
 * @author Tony.Ma
 * @date Jan. 14, 2021
 *
 */

public class ProcessV2ToJson implements Processor {
	
	private static final Logger logger = LoggerFactory.getLogger(ProcessV2ToJson.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {
		logger.debug("Trying to create a JSON Message.");
		
		Objects.requireNonNull(exchange.getIn().getBody(),"The HL7 shouldn't be null from the request");
		String hl7 = exchange.getIn().getBody().toString();
		if(!StringUtil.isNullOrEmpty(hl7)) {
			//set the message body to JSON
			exchange.getIn().setBody(FHIRJsonUtil.createFHIRJsonObj(hl7).toString());
		}
		
	}
}
