package ca.bc.gov.hlth.hnclientv2.handshakeserver;

import java.util.Random;

import ca.bc.gov.hlth.hnclientv2.error.MessageUtil;

public class HandshakeUtil {

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
    
    
	/**
	 * Generates a random array to compute challange string
	 * 
	 * @param handShakeData
	 * @return
	 */
	public String generateHandshakeData(byte[] handShakeData) {
		
		String ret_code = MessageUtil.HNET_RTRN_SUCCESS;

		if (handShakeData == null)
			ret_code = MessageUtil.HNET_RTRN_INVALIDPARAMETER;
		else {
			// create random object
			Random r = new Random(System.currentTimeMillis());
			// put the next byte in the array
			r.nextBytes(handShakeData);
		}
		//logger.debug("{} - Generated random byte array {}", methodName, ret_code);
		return ret_code;
	}
}
