package ca.bc.gov.hlth.hnclientv2.handshakeserver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HandshakeUtilTest {
	private static byte XFER_HANDSHAKE_SEED = 0;

	private static String HNET_RTRN_SUCCESS = "HNET_RTRN_SUCCESS";
	private static String HNET_RTRN_INVALIDPARAMETER = "HNET_RTRN_INVALIDPARAMETER";

	@Test
	public void test_generateHandshakeData_returns_success() {
		byte[] handShakeData = new byte[8];
		String retCode = HandshakeUtil.generateHandshakeData(handShakeData);
		assertTrue(retCode.equals(HNET_RTRN_SUCCESS));
	}

	@Test
	public void test_generateHandshakeData_returns_invalidparameter() {
		byte[] handShakeData = null;
		String retCode = HandshakeUtil.generateHandshakeData(handShakeData);
		assertTrue(retCode.equals(HNET_RTRN_INVALIDPARAMETER));
	}

	@Test
	public void test_scrambleData_returns_success() {
		byte[] handShakeData = new byte[8];
		HandshakeUtil.scrambleData(handShakeData, XFER_HANDSHAKE_SEED);
		assertNotNull(handShakeData);
	}

	@Test
	public void test_scrambleData_returns_exception() {
		byte[] handShakeData = null;
		assertThrows(NullPointerException.class, () -> {
			HandshakeUtil.scrambleData(handShakeData, XFER_HANDSHAKE_SEED);	
		});
		
	}

	@Test
	public void test_compareByteArray_false() {
		byte[] handShakeData = new byte[8];
		byte[] clientHandshakeData = new byte[8];

		HandshakeUtil.generateHandshakeData(handShakeData);
		clientHandshakeData = "clientdata".getBytes();

		HandshakeUtil.scrambleData(handShakeData, XFER_HANDSHAKE_SEED);
		HandshakeUtil.scrambleData(clientHandshakeData, XFER_HANDSHAKE_SEED);

		assertTrue(!HandshakeUtil.compareByteArray(clientHandshakeData, handShakeData));
	}

	@Test
	public void test_compareByteArray_true() {
		byte[] handShakeData = new byte[8];
		byte[] clientHandshakeData = new byte[8];

		HandshakeUtil.generateHandshakeData(handShakeData);
		clientHandshakeData = handShakeData;

		HandshakeUtil.scrambleData(clientHandshakeData, XFER_HANDSHAKE_SEED);

		assertTrue(HandshakeUtil.compareByteArray(clientHandshakeData, handShakeData));
	}

}
