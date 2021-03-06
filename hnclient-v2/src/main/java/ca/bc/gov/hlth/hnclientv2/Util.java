package ca.bc.gov.hlth.hnclientv2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.netty.util.internal.StringUtil;

public  class Util {

	public static void requireNonBlank(String str, String msg) {
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException(msg);
        }
    }
	
	/**
	 * return a Base64 encoding string
	 * 
	 * @param stringToEncode the string to convert to base64
	 * @return the base64 encoded string
	 */
	public static String encodeBase64(String stringToEncode) {
		if (StringUtil.isNullOrEmpty(stringToEncode))
			return null;
		return new String(Base64.getEncoder().encode(stringToEncode.getBytes()));
	}
	
	/**
	 * This method is for base64 decoding.
	 * 
	 * @param stringToDecode the base64encoded string we will convert back
	 * @return decoded base64 String
	 */
	public static String decodeBase64(String stringToDecode)  {
        if(StringUtil.isNullOrEmpty(stringToDecode)) return null;
		byte[] bytesToDecode = stringToDecode.getBytes(StandardCharsets.UTF_8);
        byte[] decodedBytes = Base64.getDecoder().decode(bytesToDecode);

		return new String(decodedBytes, StandardCharsets.UTF_8);
    }
	
	/**
	 * Takes a data buffer and scramble the contents using a simple algorithm
	 * 
	 * @param aByte The buffer the contents of which are to be scrambled. 
	 */

	public void scrambleData(byte[] aByte, byte decodeSeed) {
		aByte[0] ^= decodeSeed;
		for (int x = 1; x < aByte.length; x++) {
			aByte[x] ^= aByte[x - 1];
		}
	}

	/**
	 * <p>
	 * The new HNClient 4.1.d sends data in a scrambled form after the initial
	 * HandShaking data (12 bytes), and random 8 bytes (total 20 bytes) are sent
	 * This method unscrambles the data using exclusive OR and a decode seed The
	 * decode seed is derived from the random set of 8 bytes (it is also used in
	 * data scrambling)
	 *
	 * @param scrambleByte byte[] the byte array to be unscrambled
	 * @return scrambled string.
	 */

	public  String unScrambleData(byte[] scrambleByte,byte decodeSeed) {

		byte prevByte = scrambleByte[0];
		scrambleByte[0] ^= decodeSeed;
		for (int x = 1; x < scrambleByte.length; ++x) {
			byte currByte = scrambleByte[x];
			scrambleByte[x] ^= prevByte;
			prevByte = currByte;
		}
		return new String(scrambleByte);
	}

	/**
	 * @param clientData   the byte array received from pos
	 * @param originalData the original byte array
	 * @return client data and original data are equal or not.
	 */
	public  boolean compareByteArray(byte[] clientData, byte[] originalData) {
		if (clientData == originalData) {
			return true;
		}

		if (clientData == null || originalData == null) {
			return false;
		}

		int length = clientData.length;
		if (originalData.length != length) {
			return false;
		}
		for (int i = 0; i < length; i++) {
			if (clientData[i] != originalData[i]) {
				return false;
			}
		}
		return true;
	}

}
