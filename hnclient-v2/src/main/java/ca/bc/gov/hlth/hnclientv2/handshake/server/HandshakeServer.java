package ca.bc.gov.hlth.hnclientv2.handshake.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.Consume;
import org.apache.camel.Converter;
import org.apache.camel.EndpointInject;
import org.apache.camel.Handler;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandshakeServer {

	private static int XFER_HANDSHAKE_SEED = 0;
	private static int XFER_HANDSHAKE_SIZE = 8;
	private static String XFER_HS_SEGMENT = "HS";
	private static String HNET_RTRN_SUCCESS = "HNET_RTRN_SUCCESS";
	private static String HNET_RTRN_INVALIDPARAMETER = "HNET_RTRN_INVALIDPARAMETER";
	private static String HNET_RTRN_INVALIDFORMATERROR = "HNET_RTRN_INVALIDFORMATERROR";

	private static byte decodeSeed = 0;

	private static Logger logger = LoggerFactory.getLogger(HandshakeServer.class);

	@EndpointInject("direct:start")

	public HandshakeServer() {
		System.out.println("Constructor is called");
		startServer();

	}

	public void startServer() {

		Runnable serverTask = new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket mysocket = new ServerSocket(5555);

					while (true) {
						Socket connectionSocket = mysocket.accept();

						BufferedInputStream socketInput = new BufferedInputStream(connectionSocket.getInputStream(),
								1000);
						BufferedOutputStream socketOutput = new BufferedOutputStream(
								connectionSocket.getOutputStream());
						BufferedReader r = new BufferedReader(
								new InputStreamReader(socketInput, StandardCharsets.UTF_8));

						String ret_code = xfer_ReceiveHSSegment(socketOutput, socketInput, XFER_HANDSHAKE_SEED);

						System.out.println("Handshake is done with the message: {}" + ret_code);

						if (ret_code.contentEquals(HNET_RTRN_SUCCESS)) {
							// socketInput.read();
							// read SI segment
							if (socketInput.available() > 0) {
								byte[] message = new byte[44];

								socketInput.read(message);
								unScrambleData(message);

								String output = new String(message, "UTF-8");
								Scanner s = new Scanner(output);
								while (s.hasNextLine()) {
									System.out.println(s.nextLine());
								}
							}

							// read dtsegment header
							if (socketInput.available() > 0) {
								byte[] dtsegment = new byte[12];
								socketInput.read(dtsegment, 0, 12);
								unScrambleData(dtsegment);

								String output1 = new String(dtsegment, "UTF-8");
								Scanner s1 = new Scanner(output1);
								while (s1.hasNextLine()) {
									System.out.println(s1.nextLine());
								}
							}

							//read dtsegment data
							if (socketInput.available() > 0) {

								byte[] message2 = socketInput.readAllBytes();
								unScrambleData(message2);

								String output2 = new String(message2, "UTF-8");
								Scanner s2 = new Scanner(output2);
								while (s2.hasNextLine()) {
									System.out.println(s2.nextLine());
								}
							}

						}
						System.out.println("HL7 transaction is done");
						logger.debug("HL7 transaction is done: {}", ret_code);

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Thread serverThread = new Thread(serverTask);
		serverThread.start();

	}

	/*
	 * public static void main(String args[]) {
	 * 
	 * System.out.println(" Server is Running  ");
	 * logger.debug("HandShakeServer is running"); try { ServerSocket mysocket = new
	 * ServerSocket(5555);
	 * 
	 * while (true) { Socket connectionSocket = mysocket.accept();
	 * 
	 * BufferedInputStream socketInput = new
	 * BufferedInputStream(connectionSocket.getInputStream(), 1000);
	 * BufferedOutputStream socketOutput = new
	 * BufferedOutputStream(connectionSocket.getOutputStream()); //
	 * System.out.println(producer.getCurrentCacheSize()); //
	 * producer.sendBody("netty:tcp://" + "127.0.0.1" + ":" + 5555 + "?sync=false",
	 * // "hell0");
	 * 
	 * String ret_code = xfer_ReceiveHSSegment(socketOutput, socketInput,
	 * XFER_HANDSHAKE_SEED);
	 * 
	 * if (ret_code.equals(HNET_RTRN_SUCCESS)) {
	 * System.out.println("handshake is successful"); byte[] message = new byte[12];
	 * 
	 * socketInput.read(message, 0, 12); for (int i = 0; i < message.length; i++) {
	 * System.out.println((char) message[i]); } System.out.println(message); }
	 * System.out.println("Handshake is done with the message: {}" + ret_code);
	 * logger.debug("Handshake is done with the message: {}", ret_code);
	 * 
	 * } } catch (Exception e) { e.printStackTrace(); } }
	 */

	/*----------------------------------------------------------------------------
	 *This function computes the original handshake challenge data for the 
	 * listener and sends this back to the originator. It then waits for the
	 * originator's response, and when the response is received it is compared
	 * with what is expected. If a match occurs, the computed handshake seed 
	 * is returned
	 *@return success/failure message
	 */

	protected static String xfer_ReceiveHSSegment(BufferedOutputStream bos, BufferedInputStream ios,
			Integer handshakeSeed) throws Exception {
		String retCode = HNET_RTRN_SUCCESS;

		byte handshakeData[] = new byte[XFER_HANDSHAKE_SIZE];
		byte clientHandshakeData[] = new byte[XFER_HANDSHAKE_SIZE];

		if (handshakeSeed == null)
			return HNET_RTRN_INVALIDPARAMETER;

		/** Create the initial handshake data. **/

		if (retCode.equals(HNET_RTRN_SUCCESS))
			retCode = generateHandshakeData(handshakeData);

		/** Now send the Handshake Segment Header. */

		if (retCode.equals(HNET_RTRN_SUCCESS)) {
			bos.write("HS0000000008".getBytes(), 0, 12);
			bos.flush();
		}

		/** Now send the actual handshake data */

		if (retCode == HNET_RTRN_SUCCESS) {
			bos.write(handshakeData);
			bos.flush();

			System.out.println(" Sent HS Data Block to originator");

		}

		/*
		 * Now receive and verify the originator's response to the handshake data. This
		 * must be the first segment which the originator sends.
		 */

		if (retCode == HNET_RTRN_SUCCESS)
			retCode = recvVerifyHandshakeData(ios, handshakeData, clientHandshakeData);

		if (retCode.equals(HNET_RTRN_SUCCESS))
			handshakeSeed = XFER_HANDSHAKE_SIZE - 1;
		else
			System.out.println("Error recieveing handshakedata");

		return retCode;
	}

	protected static String recvVerifyHandshakeData(BufferedInputStream ios, byte[] handShakeData,
			byte[] clientHandshakeData) throws IOException, InterruptedException {

		String funcName = "RecvVerifyHandshakeData";
		String retCode = HNET_RTRN_SUCCESS;
		System.out.println("inside recvVerifyHandshakeData");

		/*
		 * Check parameters.
		 */

		if (handShakeData == null) {
			logger.debug("OrigninalHandshakeData parameter cannot be NULL");
			retCode = HNET_RTRN_INVALIDPARAMETER;
			return retCode;

		}

		if (clientHandshakeData == null) {
			logger.debug("ClientHandshakeData parameter cannot be NULL" + funcName + "HNET_RTRN_INVALIDPARAMETER");
			retCode = HNET_RTRN_INVALIDPARAMETER;

			return retCode;

		}

		/*
		 * First get the originator handshake segment header and data.
		 */
		if (retCode == HNET_RTRN_SUCCESS) {

			byte[] clientHandshakesegment = new byte[12];

			// read and verify the handshake header
			ios.read(clientHandshakesegment, 0, 12);

			if (!compareByteArray(clientHandshakesegment, "HS0000000008".getBytes())) {
				logger.debug("Handshake header segment are not matched");
				return "HNET_RTRN_INVALIDFORMATERROR";
			}

			// read and verify the handshake data
			ios.read(clientHandshakeData, 0, 8);
			retCode = verifyHandshakeResponse(clientHandshakeData, handShakeData, XFER_HANDSHAKE_SIZE);
		}
		return retCode;
	}

	/**
	 * @param clientHandshakeData
	 * @param originalHandshakeData
	 * @param XFER_HANDSHAKE_SIZE
	 * @return Suucess/ Failure message
	 */
	public static String verifyHandshakeResponse(byte[] clientHandshakeData, byte[] originalHandshakeData,
			int XFER_HANDSHAKE_SIZE) {
		String retCode = HNET_RTRN_SUCCESS;

		// Scramble the original handshake data
		scrambleData(originalHandshakeData, XFER_HANDSHAKE_SIZE, XFER_HANDSHAKE_SEED, XFER_HS_SEGMENT);

		// Compare client handshake data and original handshake data
		if (!compareByteArray(clientHandshakeData, originalHandshakeData)) {
			logger.debug("Received handshake data does not match expected data");
			retCode = HNET_RTRN_INVALIDFORMATERROR;
		} else
			System.out.println("Received handshake data matched expected data");

		return retCode;
	}

	public static String generateHandshakeData(byte[] handShakeData) {
		String ret_code = HNET_RTRN_SUCCESS;
		if (handShakeData == null)
			ret_code = HNET_RTRN_INVALIDPARAMETER;
		else {
			// create random object
			Random r = new Random(System.currentTimeMillis());
			// put the next byte in the array
			r.nextBytes(handShakeData);
		}
		return ret_code;
	}

	/**
	 * Takes a data buffer and scramble the contents using a simple algorithm
	 * 
	 * @param dataBuffer   The buffer the contents of which are to be scrambled. May
	 *                     not be NULL.
	 * @param scrambleLen  The number of bytes in the dataBuffer parameter which are
	 *                     to be scrambled. Must be <= the actual size of
	 *                     dataBuffer.
	 *
	 * @param scrambleSeed The seed value used to start the scrambling process.
	 * @param segmentType  What type of HL7XFER segment data is in dataBuffer. Must
	 *                     be one of of the XFER_XX_SEGMENT values.
	 * @param traceBuffer  The buffer to place diagnostic and tracing information.
	 */

	public static String scrambleData(byte[] dataBuffer, int scrambleLen, int scrambleSeed, String segmentType) {

		String retCode = HNET_RTRN_SUCCESS;

		/* Check the parameters for correctness. */
		if (dataBuffer == null) {
			return retCode = HNET_RTRN_INVALIDPARAMETER;
		}

		/*
		 * Now scramble the data using a simple algorithm
		 */

		if (retCode == HNET_RTRN_SUCCESS) {
			dataBuffer[0] ^= scrambleSeed;
			for (int i = 1; i < scrambleLen; i++)
				dataBuffer[i] ^= dataBuffer[i - 1];
		}

		return retCode;
	}

	/**
	 * <p>
	 * The new HNClient 4.1.d sends data in a scrambled form after the initial
	 * HandShaking data (12 bytes), and random 8 bytes (total 20 bytes) are sent
	 * This method unscrambles the data using exclusive OR and a decode seed The
	 * decode seed is derived from the random set of 8 bytes (it is also used in
	 * data scrambling)
	 *
	 * @param scrambleByte byte[]
	 * @return
	 */
	public String unScrambleData1(byte[] scrambleByte, int unscrambleLen, byte unscrambleSeed, String segmentType) {
		if (scrambleByte == null)
			return HNET_RTRN_INVALIDPARAMETER;

		byte prevByte = scrambleByte[0];
		scrambleByte[0] ^= unscrambleSeed;
		for (int x = 1; x < scrambleByte.length; x++) {
			byte currByte = scrambleByte[x];
			scrambleByte[x] ^= prevByte;
			prevByte = currByte;
		}
		return new String(scrambleByte);
	}

	public String unScrambleData(byte[] scrambleByte) {

		byte prevByte = scrambleByte[0];
		scrambleByte[0] ^= decodeSeed;
		for (int x = 1; x < scrambleByte.length; ++x) {
			byte currByte = scrambleByte[x];
			scrambleByte[x] ^= prevByte;
			prevByte = currByte;
		}
		return new String(scrambleByte);
	}

	public static boolean compareByteArray(byte[] clietData, byte[] originalData) {
		if (clietData == originalData)
			return true;
		if (clietData == null || originalData == null)
			return false;

		int length = clietData.length;
		if (originalData.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (clietData[i] != originalData[i])
				return false;

		return true;
	}

	private static byte[] readStream(InputStream is) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buff = new byte[1024];
		int len = is.read(buff);
		while (len > 0) {
			baos.write(buff, 0, len);
			if (len == buff.length) {
				len = is.read(buff);
			} else {
				len = 0;
			}
		}
		return baos.toByteArray();
	}

}
