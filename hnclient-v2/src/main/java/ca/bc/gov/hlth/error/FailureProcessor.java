package ca.bc.gov.hlth.error;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailureProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(FailureProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		String defaultErrorMessage = ErrorBuilder.buildDefaultErrorMessage("Connection refused with HNSecure");
		exchange.getIn().setBody(defaultErrorMessage);
	}
}
