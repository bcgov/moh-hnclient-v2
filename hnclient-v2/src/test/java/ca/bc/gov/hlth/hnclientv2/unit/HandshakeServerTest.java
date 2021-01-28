package ca.bc.gov.hlth.hnclientv2.unit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Base64;

import ca.bc.gov.hlth.hnclientv2.handshake.client.HandshakeClient;
import ca.bc.gov.hlth.hnclientv2.handshake.server.HandshakeServer;


public class HandshakeServerTest {
	private static int XFER_HANDSHAKE_SEED = 0;
	private static int XFER_HANDSHAKE_SIZE = 8;
	private static String XFER_HS_SEGMENT = "HS";
    
    private static String HNET_RTRN_SUCCESS = "HNET_RTRN_SUCCESS";
	private static String HNET_RTRN_INVALIDPARAMETER = "HNET_RTRN_INVALIDPARAMETER";
	private static String HNET_RTRN_INVALIDFORMATERROR = "HNET_RTRN_INVALIDFORMATERROR";

    @Test
    public void test_generateHandshakeData_returns_success() {
    	byte[] handShakeData = new byte[8];   	
    	String retCode = HandshakeServer.generateHandshakeData(handShakeData);    
    	assertTrue(retCode.equals(HNET_RTRN_SUCCESS));
    }
    
    @Test
    public void test_generateHandshakeData_returns_invalidparameter() {
    	byte[] handShakeData = null;   	
    	String retCode = HandshakeServer.generateHandshakeData(handShakeData);    
    	assertTrue(retCode.equals(HNET_RTRN_INVALIDPARAMETER));
    }
    
    @Test
    public void test_scrambleData_returns_success() {
    	byte[] handShakeData = new byte[8];
    	int scrambleLen = XFER_HANDSHAKE_SIZE;
    	String segmentType = XFER_HS_SEGMENT;
    	int scrambleSeed = XFER_HANDSHAKE_SEED;
    	
    	String retCode = HandshakeServer.scrambleData(handShakeData, scrambleLen, scrambleSeed, segmentType);    
    	assertTrue(retCode.equals(HNET_RTRN_SUCCESS));
    }
    
    @Test
    public void test_scrambleData_returns_invalidparameter() {
    	byte[] handShakeData = null;
    	int scrambleLen = XFER_HANDSHAKE_SIZE;
    	String segmentType = XFER_HS_SEGMENT;
    	int scrambleSeed = XFER_HANDSHAKE_SEED;
    	
    	String retCode = HandshakeServer.scrambleData(handShakeData, scrambleLen, scrambleSeed, segmentType);    
    	assertTrue(retCode.equals(HNET_RTRN_INVALIDPARAMETER));
    }
    
    @Test
    public void test_verifyHandshakeResponse_success() {
    	byte[] handShakeData = new byte[8];
    	byte[] clientHandshakeData = new byte[8];
    	int scrambleLen = XFER_HANDSHAKE_SIZE;
    	String segmentType = XFER_HS_SEGMENT;
    	int scrambleSeed = XFER_HANDSHAKE_SEED;
    	
    	 HandshakeServer.generateHandshakeData(handShakeData);
    	 clientHandshakeData = handShakeData;
    	
    	HandshakeServer.scrambleData(handShakeData, scrambleLen, scrambleSeed, segmentType);  
    	HandshakeClient.scrambleData(clientHandshakeData);
    	
    	String ret_code = HandshakeServer.verifyHandshakeResponse(clientHandshakeData, handShakeData, XFER_HANDSHAKE_SEED);
    	assertTrue(ret_code.equals(HNET_RTRN_SUCCESS));
    }
    
    @Test
    public void test_verifyHandshakeResponse_invalidformaterror() {
    	byte[] handShakeData = new byte[8];
    	byte[] clientHandshakeData = new byte[8];
    	int scrambleLen = XFER_HANDSHAKE_SIZE;
    	String segmentType = XFER_HS_SEGMENT;
    	int scrambleSeed = XFER_HANDSHAKE_SEED;
    	
    	HandshakeServer.generateHandshakeData(handShakeData);
    	
    	HandshakeServer.scrambleData(handShakeData, scrambleLen, scrambleSeed, segmentType);  
    	HandshakeClient.scrambleData(clientHandshakeData);
    	
    	String ret_code = HandshakeServer.verifyHandshakeResponse(clientHandshakeData, handShakeData, XFER_HANDSHAKE_SEED);
    	assertTrue(ret_code.equals(HNET_RTRN_INVALIDFORMATERROR));
    }
    /**
     * @param base64EncodedString
     * @return
     * @throws Exception
     */
    public byte[] ConvertFromBase64String(String base64EncodedString) throws Exception {
        if (base64EncodedString == null || base64EncodedString.length() == 0)
            throw new IllegalArgumentException("You must supply byte string for Base64 decoding operation");

        if (base64EncodedString.length() % 4 != 0)
            throw new IllegalArgumentException("The BASE-64 encoded data is not in correct form (divide by 4 resulted in a remainder)");

        try {
            return Base64.getDecoder().decode(base64EncodedString);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to decode Base-64 string supplied for operation. Please check your inputs");
        }
    }


}
