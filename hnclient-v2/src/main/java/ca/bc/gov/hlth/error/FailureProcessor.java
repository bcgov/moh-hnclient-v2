package ca.bc.gov.hlth.error;

import ca.bc.gov.hlth.hnclientv2.MessageUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailureProcessor implements Processor {
	private String msg;

	public FailureProcessor() {

	}
	
	public FailureProcessor(String msg) {
		this.msg=msg;
		// TODO Auto-generated constructor stub
	}

	private static final Logger logger = LoggerFactory.getLogger(FailureProcessor.class);

	@Override
	public void process(Exchange exchange) {
		logger.debug("Failure Processor is called");

		// The message was caused by caused by a failure to connect to a downstream service and should give a SERVER_NO_CONNECTION error
		// TODO Pick a single way to determine error type this could be moved to login in onException in the Route
		//  the thinking here was to have a set of subclasses of a CamelCustomException and handle the different error message types here
		//  Rather than with a bunch of onException methods in the route
		if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class) instanceof ServerNoConnectionException) {
			msg = MessageUtil.SERVER_NO_CONNECTION;
		}
		// Give the default error
		String defaultErrorMessage = ErrorBuilder.buildDefaultErrorMessage(this.msg);
		exchange.getIn().setBody(defaultErrorMessage);
	}
}
