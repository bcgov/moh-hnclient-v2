/**
 *
 */
package ca.bc.gov.hlth.hnclientv2.wrapper;

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
    public String convertToBase64String(String v2Message) {

        // TODO it should be impossible for the body to be empty here (the handshake server should catch that)
        // if we keep this we should throw an exception that causes an HL7Error_Msg_NoInputHL7 response if it is
        if (StringUtil.isNullOrEmpty(v2Message)) {
            throw new IllegalArgumentException("v2Message can't be null or empty");
        } else {
            return new String(Base64.getEncoder().encode(v2Message.getBytes()));
        }

    }

}
	

