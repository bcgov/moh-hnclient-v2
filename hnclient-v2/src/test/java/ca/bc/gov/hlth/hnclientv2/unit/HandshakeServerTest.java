package ca.bc.gov.hlth.hnclientv2.unit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ca.bc.gov.hlth.hnclientv2.Util;
import ca.bc.gov.hlth.hnclientv2.handshake.server.HandshakeServer;

public class HandshakeServerTest {
	private static byte XFER_HANDSHAKE_SEED = 0;

	private static String HNET_RTRN_SUCCESS = "HNET_RTRN_SUCCESS";
	private static String HNET_RTRN_INVALIDPARAMETER = "HNET_RTRN_INVALIDPARAMETER";
	private static final Util util = new Util();

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
		util.scrambleData(handShakeData, XFER_HANDSHAKE_SEED);
		assertNotNull(handShakeData);
	}

	@Test(expected = NullPointerException.class)
	public void test_scrambleData_returns_exception() {
		byte[] handShakeData = null;

		util.scrambleData(handShakeData, XFER_HANDSHAKE_SEED);
	}

	@Test
	public void test_compareByteArray_false() {
		byte[] handShakeData = new byte[8];
		byte[] clientHandshakeData = new byte[8];

		HandshakeServer.generateHandshakeData(handShakeData);
		clientHandshakeData = "clientdata".getBytes();

		util.scrambleData(handShakeData, XFER_HANDSHAKE_SEED);
		util.scrambleData(clientHandshakeData, XFER_HANDSHAKE_SEED);

		assertTrue(!util.compareByteArray(clientHandshakeData, handShakeData));
	}

	@Test
	public void test_compareByteArray_true() {
		byte[] handShakeData = new byte[8];
		byte[] clientHandshakeData = new byte[8];

		HandshakeServer.generateHandshakeData(handShakeData);
		clientHandshakeData = handShakeData;

		util.scrambleData(clientHandshakeData, XFER_HANDSHAKE_SEED);

		assertTrue(util.compareByteArray(clientHandshakeData, handShakeData));
	}

}
