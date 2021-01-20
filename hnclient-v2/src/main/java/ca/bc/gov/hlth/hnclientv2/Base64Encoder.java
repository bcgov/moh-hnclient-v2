/**
 * 
 */
package ca.bc.gov.hlth.hnclientv2;

import java.util.Base64;

import org.apache.camel.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.StringUtil;

/**
 * @author anumeha.srivastava
 *
 */

public class Base64Encoder {

	private static Logger logger = LoggerFactory.getLogger(Base64Encoder.class);
		
	@Handler
	public static String convertToBase64String(String vMessage) {
		
		String encodedString = encodeString(vMessage);
		logger.info("Message encoded successfully");
		return encodedString;
		
	}
	
	

	public static String encodeString(String v2Message) {
	if(StringUtil.isNullOrEmpty(v2Message)){
		throw new IllegalArgumentException("v2Message can't be null or empty");
	} else
	{
		return new String(Base64.getEncoder().encode(v2Message.getBytes()));

	}
	}
	
	

}
	

