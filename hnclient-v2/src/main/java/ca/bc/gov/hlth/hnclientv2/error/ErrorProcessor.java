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
			if (keySet.contains(header)) {

				String customError = ErrorBuilder.generateError(header, null);

				logger.debug("Error is:" + customError);
				exchange.getIn().setBody(customError);
			} else if (header == null) {

				String defaultErrorMessage = ErrorBuilder.buildDefaultErrorMessage("An unknown error has occurred.");

				exchange.getIn().setBody(defaultErrorMessage);

			}

		} catch (Exception e) {
			logger.debug("{} - Error occured :{}", methodName, e.getMessage());
			throw new CamelCustomException("An unknown error has occurred.");
		}

	}

}
