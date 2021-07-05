package ca.bc.gov.hlth.hnclientv2.error;

import java.net.UnknownHostException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hncommon.util.LoggingUtil;

public class FailureProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(FailureProcessor.class);

	@Override
	public void process(Exchange exchange) {
		logger.debug("{} - Failure Processor is called", LoggingUtil.getMethodName());
		
		Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		String errMsg = MessageUtil.UNKNOWN_EXCEPTION;

		if (exception instanceof IllegalArgumentException) {
			errMsg = MessageUtil.INVALID_PARAMETER;
		} else if (exception instanceof HttpHostConnectException) {
			errMsg = MessageUtil.SERVER_NO_CONNECTION;
		} else if (exception instanceof UnknownHostException) {			
			errMsg = MessageUtil.SERVER_NO_CONNECTION;
		} else if (exception instanceof NoInputHL7Exception) {
			errMsg = MessageUtil.HL7_ERROR_MSG_NO_INPUT_HL7;
		} else if (exception instanceof ServerNoConnectionException) {
			// The message was caused by caused by a failure to connect to a downstream service and should give a SERVER_NO_CONNECTION error
			errMsg = MessageUtil.SERVER_NO_CONNECTION;
		} else if (exception instanceof CamelCustomException) {
			errMsg = MessageUtil.UNKNOWN_EXCEPTION;
		}
		
		logger.error("{} - Processing Exception of type {} with message {}", LoggingUtil.getMethodName(), getClass().getName(), errMsg);
		
		// Give the default error
		String defaultErrorMessage = ErrorBuilder.buildErrorMessage("", errMsg);
		exchange.getIn().setBody(defaultErrorMessage);
	}
}
