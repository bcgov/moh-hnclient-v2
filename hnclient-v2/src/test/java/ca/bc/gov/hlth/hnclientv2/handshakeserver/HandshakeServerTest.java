package ca.bc.gov.hlth.hnclientv2.handshakeserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import ca.bc.gov.hlth.hnclientv2.TransactionIdGenerator;

public class HandshakeServerTest {
	private static final String LOCAL_IP_ADDRESS = "127.0.0.1";

	/**
	 * Basic test that ensures that the server is up on the configured port and able to read/write data via a ConnectionHandler.
	 * More detailed testing should be done via a ConnectionHandler test but this will require extensive development.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testInitConnectionHandlers_success() throws UnknownHostException, IOException, InterruptedException {
		ServerProperties properties = initServerProperties();		
		new HandshakeServer(null, properties);

		try (Socket netSocket = new Socket(LOCAL_IP_ADDRESS, properties.getServerSocket());
				BufferedInputStream socketInput = new BufferedInputStream(netSocket.getInputStream(), 1000);
				BufferedOutputStream socketOutput = new BufferedOutputStream(netSocket.getOutputStream())) {

    		int numSocketReadTries = 0;
    		
			// Wait for data from HNClient
			while (socketInput.available() < 1 && numSocketReadTries < properties.getMaxSocketReadTries()) {
				numSocketReadTries++;
				java.lang.Thread.sleep(properties.getSocketReadSleepTime());
			}
    		
			// Validate that the socket is open and ready to read
			assertTrue(socketInput.available() > 0);

			// Read 12 bytes of HandShake Segment and 8 bytes of HandShake data
			byte handShakeSegment[] = new byte[12];
			byte handShakeData[] = new byte[8];

			socketInput.read(handShakeSegment, 0, 12);
			socketInput.read(handShakeData, 0, 8);
			
			// Assert that the handshakeSegment is non-zero
			assertFalse(Arrays.equals(handShakeSegment, new byte[handShakeSegment.length]));
			// Assert that the handshakeData is non-zero
			assertFalse(Arrays.equals(handShakeSegment, new byte[handShakeData.length]));

			// write 12 bytes of HS response to BufferedOutputStream
			// This should not raise an exception
			socketOutput.write("HS0000000008".getBytes(), 0, 12); 
			
			// Don't test anything further as it requires lots of framework code for dealing with the
			// handshake protocol as well as a valid Producer. It's also best suited for a ConnectionHandlerTest.
    	}
	}
	
	/**
	 * Tests that requests can't be handled when no threads are configured.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testInitConnectionHandlers_noThreads() throws IOException {
		ServerProperties properties = initServerProperties();
		String transactionId = new TransactionIdGenerator().generateUuid();
		// Start it on a different port since the other test cases will start
		// ports that won't shut down until the test terminates.
		properties.setServerSocket(5656);
		properties.setThreadPoolSize(0);
		new HandshakeServer(null, properties);
		
    	assertThrows(ConnectException.class, () -> {
    		new Socket(LOCAL_IP_ADDRESS, properties.getServerSocket());
    	});

	}
	
	private ServerProperties initServerProperties() throws IOException {
		InputStream input = HandshakeServerTest.class.getClassLoader().getResourceAsStream("application.properties");
		Properties props = new Properties();
		props.load(input);
		
		Integer port = Integer.valueOf((String)props.get("server-socket"));
		Integer socketReadSleepTime = Integer.valueOf((String)props.get("socket-read-sleep-time"));
		Integer maxSocketReadTries = Integer.valueOf((String)props.get("max-socket-read-tries"));
		Integer threadPoolSize = Integer.valueOf((String)props.get("thread-pool-size"));
		Boolean acceptRemoteConnections = Boolean.valueOf((String)props.get("accept-remote-connection"));
		String validIpListFile = (String)props.get("valid-ip-list-file");
		return new ServerProperties(port, socketReadSleepTime, maxSocketReadTries, threadPoolSize, acceptRemoteConnections, validIpListFile);
	}

}
