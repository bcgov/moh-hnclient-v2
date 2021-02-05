package ca.bc.gov.hlth.hnclientv2.unit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ca.bc.gov.hlth.hnclientv2.handshake.client.HandshakeClient;
import ca.bc.gov.hlth.hnclientv2.handshake.server.HandshakeServer;

public class HandshakeServerTest {
	private static int XFER_HANDSHAKE_SEED = 0;

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
		HandshakeServer.scrambleData(handShakeData);
		assertNotNull(handShakeData);
	}

	@Test(expected = NullPointerException.class)
	public void test_scrambleData_returns_exception() {
		byte[] handShakeData = null;

		HandshakeServer.scrambleData(handShakeData);
	}

	@Test
	public void test_verifyHandshakeResponse_success() {
		byte[] handShakeData = new byte[8];
		byte[] clientHandshakeData = new byte[8];

		HandshakeServer.generateHandshakeData(handShakeData);
		clientHandshakeData = handShakeData;

		HandshakeServer.scrambleData(handShakeData);
		HandshakeClient.scrambleData(clientHandshakeData);

		String ret_code = HandshakeServer.verifyHandshakeResponse(clientHandshakeData, handShakeData,
				XFER_HANDSHAKE_SEED);
		assertTrue(ret_code.equals(HNET_RTRN_SUCCESS));
	}

	@Test
	public void test_verifyHandshakeResponse_invalidformaterror() {
		byte[] handShakeData = new byte[8];
		byte[] clientHandshakeData = new byte[8];

		HandshakeServer.generateHandshakeData(handShakeData);

		HandshakeServer.scrambleData(handShakeData);
		HandshakeClient.scrambleData(clientHandshakeData);

		String ret_code = HandshakeServer.verifyHandshakeResponse(clientHandshakeData, handShakeData,
				XFER_HANDSHAKE_SEED);
		assertTrue(ret_code.equals(HNET_RTRN_INVALIDFORMATERROR));
	}

}
