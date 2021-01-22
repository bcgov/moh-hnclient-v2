package ca.bc.gov.hlth.hnclientv2;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.netty.util.internal.StringUtil;

public final class Util {

    private Util() {
    }

	public static void requireNonBlank(String str, String msg) {
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException(msg);
        }
    }
	
	/**
	 * return a Base64 encoding string
	 * 
	 * @param stringToEncode
	 * @return
	 */
	public static String encodeBase64(String stringToEncode) {
		if (StringUtil.isNullOrEmpty(stringToEncode))
			return null;
		return new String(Base64.getEncoder().encode(stringToEncode.getBytes()));
	}
	
	/**
	 * This method is for base64 decoding.
	 * 
	 * @param stringToDecode
	 * @return 64Encoding String
	 * @throws UnsupportedEncodingException
	 */
	public static String decodeBase64(String stringToDecode) throws UnsupportedEncodingException {
        if(StringUtil.isNullOrEmpty(stringToDecode)) return null;
		byte[] bytesToDecode = stringToDecode.getBytes(StandardCharsets.UTF_8);
        byte[] decodedBytes = Base64.getDecoder().decode(bytesToDecode);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

        return decodedString;
    }

}
