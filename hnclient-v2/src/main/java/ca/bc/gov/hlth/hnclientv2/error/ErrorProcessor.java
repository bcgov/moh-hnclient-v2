package ca.bc.gov.hlth.hnclientv2.error;

import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anumeha.srivastava
 *
 */
public class ErrorProcessor implements Processor {
	private static final Logger logger = LoggerFactory.getLogger(ErrorProcessor.class);
	Map<Integer, ErrorCodes> errorCodeByErrorNumber = ErrorCodes.errorCodeByErrorNumber;

	@Override
	public void process(Exchange exchange) throws CamelCustomException {
		
		String methodName = "process";
		
		try {
			Message in = exchange.getIn();

			Map<String, Object> headers = in.getHeaders();

			Integer header = (Integer) headers.get("CamelHttpResponseCode");

			Set<Integer> keySet = errorCodeByErrorNumber.keySet();

			logger.info("{} - Received http status code is: ", methodName, header);
			String body = null;
			if (keySet.contains(header)) {
				body = ErrorBuilder.generateError(header, null);
				exchange.getIn().setBody(body);
			} else if (header == null) {
				body = ErrorBuilder.buildDefaultErrorMessage(MessageUtil.UNKNOWN_ERROR);
				exchange.getIn().setBody(body);
			}
			logger.debug("{} - Set the body of exchange as {}", methodName, body);
		} catch (Exception e) {
			logger.error("{} - Error occured :{}", methodName, e.getMessage());
			throw new CamelCustomException(MessageUtil.UNKNOWN_ERROR);
		}

	}

}
