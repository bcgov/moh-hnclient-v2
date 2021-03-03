package ca.bc.gov.hlth.error;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailureProcessor implements Processor {
	private String msg;
	
	public FailureProcessor(String msg) {
		this.msg=msg;
		// TODO Auto-generated constructor stub
	}

	private static final Logger logger = LoggerFactory.getLogger(FailureProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		logger.debug("Failure Processor is called");
		String defaultErrorMessage = ErrorBuilder.buildDefaultErrorMessage(this.msg);
		exchange.getIn().setBody(defaultErrorMessage);
	}
}
