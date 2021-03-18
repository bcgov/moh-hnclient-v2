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
		try {
			Message in = exchange.getIn();

			Map<String, Object> headers = in.getHeaders();

			Integer header = (Integer) headers.get("CamelHttpResponseCode");

			Set<Integer> keySet = errorCodeByErrorNumber.keySet();
			logger.info("Recieved http status code is:" + header);

			if (keySet.contains(header)) {
				logger.info("Recieved http status code is:" + header);

				String customError = ErrorBuilder.generateError(header, null);

				logger.debug("Error is:" + customError);
				exchange.getIn().setBody(customError);
			} else if (header == null || header != 200) {

				logger.debug("Recieved http status code is:" + header);
				String defaultErrorMessage = ErrorBuilder.buildDefaultErrorMessage("An unknown error has occurred.");

				logger.info("Built default error message");
				exchange.getIn().setBody(defaultErrorMessage);

			}
		} catch (Exception e) {
			throw new CamelCustomException("An unknown error has occurred.");
		}

	}

}
