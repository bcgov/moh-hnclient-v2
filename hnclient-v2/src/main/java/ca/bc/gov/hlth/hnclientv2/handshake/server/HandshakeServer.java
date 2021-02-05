package ca.bc.gov.hlth.hnclientv2.handshake.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

import org.apache.camel.EndpointInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandshakeServer {

	private static int XFER_HANDSHAKE_SEED = 0;
	private static int XFER_HANDSHAKE_SIZE = 8;
	private static String HNET_RTRN_SUCCESS = "HNET_RTRN_SUCCESS";
	private static String HNET_RTRN_INVALIDPARAMETER = "HNET_RTRN_INVALIDPARAMETER";
	private static String HNET_RTRN_INVALIDFORMATERROR = "HNET_RTRN_INVALIDFORMATERROR";
	// data indicator
	private static final String DATA_INDICATOR = "DT";
	// message header indicator
	public static final String HEADER_INDICATOR = "MSH";
	// data indicator length
	private static final int DATA_INDICATOR_LENGTH = 2;
	// length indicator length
	private static final int LENGTH_INDICATOR_LENGTH = 12;

	private static byte decodeSeed = 0;
	// DT segments to send to POS
	private static byte dataSegmentout[] = new byte[12];
	private static byte dataHL7out[];

	private static final String TEN_ZEROS = "0000000000";

	private static Logger logger = LoggerFactory.getLogger(HandshakeServer.class);

	private static String v2MsgResponse = "MSH|^~\\&|HNETDTTN|BC00002041|HNTIMEAP|BC01000161|20210203164324-0800|ANUMEHA.SRIVASTAVA|NMR||D|2.3||||\\0x0dMSA|AA|||||\\0x0dNCK|20210203164324-0800\\0x0d\\0x0d";

	@EndpointInject("direct:start")

	public HandshakeServer() {
		logger.info("HandshakeServer constructor is called");
		startServer();

	}

	public void startServer() {
		final int SOCKET_READ_SLEEP_TIME = 100; // milliseconds
		final int MAX_SOCKET_READ_TRIES = 100; // total of 10 seconds

		Runnable serverTask = new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket mysocket = new ServerSocket(5555);
					String headerIn;
					while (true) {
						Socket connectionSocket = mysocket.accept();

						BufferedInputStream socketInput = new BufferedInputStream(connectionSocket.getInputStream(),
								1000);
						BufferedOutputStream socketOutput = new BufferedOutputStream(
								connectionSocket.getOutputStream());

						String ret_code = xfer_ReceiveHSSegment(socketOutput, socketInput, XFER_HANDSHAKE_SEED);

						System.out.println("Handshake is done with the message: {}" + ret_code);

						if (ret_code.contentEquals(HNET_RTRN_SUCCESS)) {
							// read SI segment
							if (socketInput.available() > 0) {
								byte[] message = new byte[44];
								socketInput.read(message);
								extractData(message);
								logger.info("Completed reading SI Segment");
							} else {
								throw new Exception("Could not find SI Segment");
							}

							// read dtsegment header
							byte[] dtsegment = new byte[12];

							socketInput.read(dtsegment, 0, 12);
							extractData(dtsegment);
							if (dtsegment.length > 0) {
								int numSocketReadTries = 0;
								headerIn = new String(dtsegment, "UTF-8");

								while (!headerIn.contains(DATA_INDICATOR)) {
									socketInput.read(dtsegment, 0, 12);
									headerIn = unScrambleData(dtsegment);

									if (numSocketReadTries < MAX_SOCKET_READ_TRIES) {
										numSocketReadTries++;
										//
										// Give other packets a chance to arrive and
										// avoid trying to monopolize CPU time..
										//
										java.lang.Thread.sleep(SOCKET_READ_SLEEP_TIME);
									} else {
										//
										// Give up eventually.
										//
										throw new Exception("Could not find DT Response.");
									}
									logger.info("Completed reading DT Segment");
								}

								// }

								// Look for the start of data
								if (headerIn.contains(DATA_INDICATOR)) {
									int messageLength = Integer.parseInt(headerIn.substring(DATA_INDICATOR_LENGTH, LENGTH_INDICATOR_LENGTH));

									numSocketReadTries = 0;

									while (socketInput.available() < messageLength) {
										if (numSocketReadTries < MAX_SOCKET_READ_TRIES) {
											numSocketReadTries++;
											//
											// Give other packets a chance to arrive and
											// avoid trying to monopolize CPU time..
											//
											java.lang.Thread.sleep(SOCKET_READ_SLEEP_TIME);
										} else {
											//
											// Give up eventually.
											//
											throw new Exception("Could not find entire message.");
										}
									}

									// read dtsegment data

									byte[] message2 = new byte[messageLength];
									socketInput.read(message2, 0, messageLength);
									extractData(message2);
									String HL7IN = new String(message2, "UTF-8");

									int indexOfMSG = HL7IN.indexOf(HEADER_INDICATOR);

									if (indexOfMSG != -1) {
										//
										// Read whatever is after the MSH segment.
										String aResponse = HL7IN.substring(indexOfMSG) + "\r";
										logger.info("HL7 message recived from POS ----" + aResponse);
									} else {
										throw new Exception("Could not find MSG");
									}

								} else {
									throw new Exception("Could not find DT Header");
								}
							} else {
								throw new Exception("Could not find DT segment");
							}

							// ************************************************************************

							// Write Response back to POS

							String dtSegment = insertHeader(v2MsgResponse);

							dataSegmentout = dtSegment.substring(0, 12).getBytes("UTF-8");

							dataHL7out = dtSegment.substring(12).getBytes("UTF-8");
							logger.info("dataHL7out: " + new String(dataHL7out));

							// scramble each segment prior to sending it to BufferedOutputStream
							scrambleData(dataSegmentout);

							socketOutput.write(dataSegmentout);

							scrambleData(dataHL7out);
							socketOutput.write(dataHL7out);
							socketOutput.flush();

							// sent Response to POS

							// ************************************************************************************

							System.out.println("HL7 transaction is done");
							logger.debug("HL7 transaction is done: {}", ret_code);

						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			private void extractData(byte[] dtsegment) throws UnsupportedEncodingException {
				unScrambleData(dtsegment);

				String output1 = new String(dtsegment, "UTF-8");
				Scanner s1 = new Scanner(output1);
				while (s1.hasNextLine()) {
					System.out.println(s1.nextLine());
				}
				s1.close();
			}
		};
		Thread serverThread = new Thread(serverTask);
		serverThread.start();

	}

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
			decodeSeed = handshakeData[XFER_HANDSHAKE_SIZE - 1];
		else
			logger.info("Error recieveing handshakedata");

		return retCode;
	}

	/**
	 * Receives the handshake segment from the originator and verifies it for
	 * correctness.
	 * 
	 * @param ios                 the input stream
	 * @param handShakeData       the original handshake data
	 * @param clientHandshakeData the client handshake data
	 * @return HNET_RTRN_SUCCESS /HNET_RTRN_INVALIDPARAMETER /
	 *         HNET_RTRN_INVALIDFORMATERROR
	 * @throws IOException          the io exception
	 * @throws InterruptedException the interrupted exception
	 */
	protected static String recvVerifyHandshakeData(BufferedInputStream ios, byte[] handShakeData,
			byte[] clientHandshakeData) throws IOException, InterruptedException {

		String funcName = "RecvVerifyHandshakeData";
		String retCode = HNET_RTRN_SUCCESS;
		logger.info("Executing recvVerifyHandshakeData method");

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
		if (retCode.equals(HNET_RTRN_SUCCESS)) {

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
	 * @param clientHandshakeData   the client handshake data
	 * @param originalHandshakeData the original server handshake data
	 * @param XFER_HANDSHAKE_SIZE   the handshake data size
	 * @return Success/ Failure message
	 */
	public static String verifyHandshakeResponse(byte[] clientHandshakeData, byte[] originalHandshakeData,
			int XFER_HANDSHAKE_SIZE) {
		String retCode = HNET_RTRN_SUCCESS;

		// Scramble the original handshake data
		scrambleData(originalHandshakeData);

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
	 * @param aByte      The buffer the contents of which are to be scrambled. May
	 * @param decodeSeed The seed value used to start the scrambling process.
	 * @return scrambled data
	 */

	public static byte[] scrambleData(byte[] aByte) {
		aByte[0] ^= decodeSeed;
		for (int x = 1; x < aByte.length; x++) {
			aByte[x] ^= aByte[x - 1];
		}

		return aByte;
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

	/**
	 * @param clietData    the byte array received from pos
	 * @param originalData the original byte array
	 * @return client data and original data are equal or not.
	 */
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

	protected String insertHeader(String aMessage) {
		String lengthOfMessage = String.valueOf(aMessage.length());
		aMessage = "DT" + TEN_ZEROS.substring(0, TEN_ZEROS.length() - lengthOfMessage.length()) + lengthOfMessage
				+ aMessage;
		return aMessage;
	}

}
