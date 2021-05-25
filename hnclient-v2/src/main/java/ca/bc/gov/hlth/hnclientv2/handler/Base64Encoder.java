/**
 *
 */
package ca.bc.gov.hlth.hnclientv2.handler;

import java.util.Base64;

import org.apache.camel.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hnclientv2.error.NoInputHL7Exception;
import ca.bc.gov.hlth.hncommon.util.LoggingUtil;
import io.netty.util.internal.StringUtil;

/**
 * @author anumeha.srivastava
 *
 */
public class Base64Encoder {

	private static Logger logger = LoggerFactory.getLogger(Base64Encoder.class);

	@Handler
	public String convertToBase64String(String v2Message) throws NoInputHL7Exception {
		logger.debug("{}: {}", LoggingUtil.getMethodName(), v2Message);

		// It should be impossible for the body to be empty here (the handshake server should catch that) but handle it just in case
		if (StringUtil.isNullOrEmpty(v2Message)) {
			throw new NoInputHL7Exception();
		}
		return new String(Base64.getEncoder().encode(v2Message.getBytes()));
	}

}