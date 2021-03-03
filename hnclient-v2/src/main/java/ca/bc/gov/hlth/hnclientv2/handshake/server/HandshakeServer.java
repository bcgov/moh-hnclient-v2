package ca.bc.gov.hlth.hnclientv2.handshake.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.error.ErrorBuilder;
import ca.bc.gov.hlth.hnclientv2.Util;
import io.netty.util.internal.StringUtil;

public class HandshakeServer {

	private static final int XFER_HANDSHAKE_SEED = 0;
	private static final int XFER_HANDSHAKE_SIZE = 8;
	private static final String HNET_RTRN_SUCCESS = "HNET_RTRN_SUCCESS";
	private static final String HNET_RTRN_INVALIDPARAMETER = "HNET_RTRN_INVALIDPARAMETER";
	private static final String HNET_RTRN_INVALIDFORMATERROR = "HNET_RTRN_INVALIDFORMATERROR";
	private static final String HL7Error_Msg_MSHSegmentMissing = "The MSH Segment from the HL7 Message is missing.";
	private static final String HL7Error_Msg_ErrorDTHeaderToHNClient = "Unable to send the DT Header to HNCLIENT.";
	private static final String HL7Error_Msg_NoInputHL7 = "No HL7 Message was supplied as input";
	private static final String HL7Error_Msg_ServerUnavailable = "Protocol error: received an unexpected data segment from HL7Xfer Server.";

	// data indicator
	private static final String DATA_INDICATOR = "DT";
	// message header indicator
	public static final String HEADER_INDICATOR = "MSH";
	// data indicator length
	private static final int DATA_INDICATOR_LENGTH = 2;
	// length indicator length
	private static final int LENGTH_INDICATOR_LENGTH = 12;

	private byte decodeSeed = 0;
	// DT segments to send to POS
	private byte[] dataSegmentout = new byte[12];
	private byte[] dataHL7out;

	private static final String TEN_ZEROS = "0000000000";

	private static final Logger logger = LoggerFactory.getLogger(HandshakeServer.class);

	private final ProducerTemplate producer;

	private static final Util util = new Util();

	public HandshakeServer(ProducerTemplate producer) {
		logger.debug("HandshakeServer constructor is called");
		this.producer = producer;
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

						logger.info("Handshake is done with the message: " + ret_code);

						String hnSecureResponse = "";

						if (ret_code.contentEquals(HNET_RTRN_SUCCESS)) {
							// read SI segment
							if (socketInput.available() > 0) {
								byte[] message = new byte[44];
								socketInput.read(message);
								extractData(message);
								logger.info("Completed reading SI Segment");
							} else {
								logger.debug("Could not find SI Segment");
								ret_code = HNET_RTRN_INVALIDFORMATERROR;
							}

							// read dtsegment header
							byte[] dtsegment = new byte[12];

							socketInput.read(dtsegment, 0, 12);
							extractData(dtsegment);
							int numSocketReadTries = 0;
							headerIn = new String(dtsegment, StandardCharsets.UTF_8);

							while (!headerIn.contains(DATA_INDICATOR)) {
								socketInput.read(dtsegment, 0, 12);
								headerIn = util.unScrambleData(dtsegment, decodeSeed);

								if (numSocketReadTries < MAX_SOCKET_READ_TRIES) {
									numSocketReadTries++;
									// Give other packets a chance to arrive and
									// avoid trying to monopolize CPU time..
									Thread.sleep(SOCKET_READ_SLEEP_TIME);
								} else {
									// Give up eventually.
									logger.debug("Could not find DT Response.");
									ret_code = HNET_RTRN_INVALIDFORMATERROR;
									hnSecureResponse = ErrorBuilder
											.buildDefaultErrorMessage(HL7Error_Msg_ErrorDTHeaderToHNClient);
								}
								logger.info("Completed reading DT Segment");
							}

							// Look for the start of data
							if (headerIn.contains(DATA_INDICATOR)) {
								int messageLength = Integer
										.parseInt(headerIn.substring(DATA_INDICATOR_LENGTH, LENGTH_INDICATOR_LENGTH));

								numSocketReadTries = 0;

								while (socketInput.available() < messageLength) {
									if (numSocketReadTries < MAX_SOCKET_READ_TRIES) {
										numSocketReadTries++;
										// Give other packets a chance to arrive and
										// avoid trying to monopolize CPU time..
										Thread.sleep(SOCKET_READ_SLEEP_TIME);
									} else {
										// Give up eventually.
										logger.debug("Could not find entire message.");
										ret_code = HNET_RTRN_INVALIDFORMATERROR;
										hnSecureResponse = ErrorBuilder
												.buildDefaultErrorMessage(HL7Error_Msg_NoInputHL7);

									}
								}

								// read dtsegment data
								byte[] message2 = new byte[messageLength];
								socketInput.read(message2, 0, messageLength);
								extractData(message2);
								String HL7IN = new String(message2, StandardCharsets.UTF_8);

								int indexOfMSG = HL7IN.indexOf(HEADER_INDICATOR);

								if (indexOfMSG != -1) {
									// Read whatever is after the MSH segment.
									String aResponse = HL7IN.substring(indexOfMSG) + "\r";
									logger.info("HL7 message recived from POS: " + aResponse);
									try {
										hnSecureResponse = (String) producer.requestBody(aResponse);
									} catch (Exception e) {
										logger.debug("Failed to send request to remote server");
										hnSecureResponse = ErrorBuilder
												.buildHTTPErrorMessage(HL7Error_Msg_ServerUnavailable, null);
									}

								} else {
									logger.debug(HL7Error_Msg_MSHSegmentMissing);
									hnSecureResponse = ErrorBuilder
											.buildHTTPErrorMessage(HL7Error_Msg_MSHSegmentMissing, null);
								}

							} else {
								logger.debug(HL7Error_Msg_ErrorDTHeaderToHNClient);
								hnSecureResponse = ErrorBuilder
										.buildDefaultErrorMessage(HL7Error_Msg_ErrorDTHeaderToHNClient);
							}

							// Write Response back to POS
							if (StringUtil.isNullOrEmpty(hnSecureResponse)) {
								hnSecureResponse = ErrorBuilder
										.buildDefaultErrorMessage(HL7Error_Msg_ServerUnavailable);
							}
							String dtSegment = insertHeader(hnSecureResponse);

							dataSegmentout = dtSegment.substring(0, 12).getBytes(StandardCharsets.UTF_8);

							dataHL7out = dtSegment.substring(12).getBytes(StandardCharsets.UTF_8);
							logger.info("dataHL7out: " + new String(dataHL7out));

							// scramble each segment prior to sending it to BufferedOutputStream
							util.scrambleData(dataSegmentout, decodeSeed);
							socketOutput.write(dataSegmentout);

							util.scrambleData(dataHL7out, decodeSeed);
							socketOutput.write(dataHL7out);
							socketOutput.flush();

							// sent Response to POS
							logger.info("HL7 transaction is done: {}", ret_code);

						} else

							logger.info("HL7 transaction is done: {}", ret_code);
						// reset decodeseed
						decodeSeed = 0;
					}
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			private void extractData(byte[] dtsegment) {
				util.unScrambleData(dtsegment, decodeSeed);

				String output1 = new String(dtsegment, StandardCharsets.UTF_8);
				Scanner s1 = new Scanner(output1);
				while (s1.hasNextLine()) {
					logger.info(s1.nextLine());
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

	protected String xfer_ReceiveHSSegment(BufferedOutputStream bos, BufferedInputStream ios, Integer handshakeSeed)
			throws Exception {

		byte[] handshakeData = new byte[XFER_HANDSHAKE_SIZE];
		byte[] clientHandshakeData = new byte[XFER_HANDSHAKE_SIZE];

		if (handshakeSeed == null) {
			return HNET_RTRN_INVALIDPARAMETER;
		}
		/* Create the initial handshake data. **/
		String retCode = generateHandshakeData(handshakeData);

		/* Now send the Handshake Segment Header. */
		if (retCode.equals(HNET_RTRN_SUCCESS)) {
			bos.write("HS0000000008".getBytes(), 0, 12);
			bos.flush();
		}

		/* Now send the actual handshake data */
		if (retCode.equals(HNET_RTRN_SUCCESS)) {
			bos.write(handshakeData);
			bos.flush();

			logger.info("Sent HS Data Block to originator");
		}

		/*
		 * Now receive and verify the originator's response to the handshake data. This
		 * must be the first segment which the originator sends.
		 */
		if (retCode.equals(HNET_RTRN_SUCCESS))
			retCode = recvVerifyHandshakeData(ios, handshakeData, clientHandshakeData);

		if (retCode.equals(HNET_RTRN_SUCCESS))
			decodeSeed = handshakeData[XFER_HANDSHAKE_SIZE - 1];

		logger.info("Error recieveing handshakedata");

		return retCode;
	}

	/**
	 * Receives the handshake segment from the originator and verifies it for
	 * correctness.
	 * 
	 * @param ios the input stream
	 * @param handShakeData the original handshake data
	 * @param clientHandshakeData the client handshake data
	 * @return HNET_RTRN_SUCCESS /HNET_RTRN_INVALIDPARAMETER /
	 *         HNET_RTRN_INVALIDFORMATERROR
	 * @throws IOException the io exception
	 */
	protected String recvVerifyHandshakeData(BufferedInputStream ios, byte[] handShakeData, byte[] clientHandshakeData)
			throws IOException {

		String funcName = "RecvVerifyHandshakeData";
		String retCode;
		logger.info("Executing recvVerifyHandshakeData method");

		// Check parameters.
		if (handShakeData == null) {
			logger.debug("Origninal HandshakeData parameter cannot be NULL");
			retCode = HNET_RTRN_INVALIDPARAMETER;
			return retCode;

		} else if (clientHandshakeData == null) {
			logger.debug("ClientHandshakeData parameter cannot be NULL" + funcName + "HNET_RTRN_INVALIDPARAMETER");
			retCode = HNET_RTRN_INVALIDPARAMETER;

			return retCode;
		} else {
			byte[] clientHandshakesegment = new byte[12];

			// read and verify the handshake header
			ios.read(clientHandshakesegment, 0, 12);

			if (!util.compareByteArray(clientHandshakesegment, "HS0000000008".getBytes())) {
				logger.debug("Handshake header segment are not matched");
				return "HNET_RTRN_INVALIDFORMATERROR";
			}

			// read and verify the handshake data
			ios.read(clientHandshakeData, 0, 8);
			retCode = verifyHandshakeResponse(clientHandshakeData, handShakeData);
		}
		return retCode;
	}

	/**
	 * @param clientHandshakeData   the client handshake data
	 * @param originalHandshakeData the original server handshake data
	 * @return Success/ Failure message
	 */
	public String verifyHandshakeResponse(byte[] clientHandshakeData, byte[] originalHandshakeData) {
		String retCode = HNET_RTRN_SUCCESS;

		// Scramble the original handshake data
		util.scrambleData(originalHandshakeData, decodeSeed);

		// Compare client handshake data and original handshake data
		if (!util.compareByteArray(clientHandshakeData, originalHandshakeData)) {
			logger.debug("Received handshake data does not match expected data");
			retCode = HNET_RTRN_INVALIDFORMATERROR;
		} else {
			logger.debug("Received handshake data matched expected data");
		}
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

	protected String insertHeader(String aMessage) {
		String lengthOfMessage = String.valueOf(aMessage.length());
		aMessage = "DT" + TEN_ZEROS.substring(0, TEN_ZEROS.length() - lengthOfMessage.length()) + lengthOfMessage
				+ aMessage;
		return aMessage;
	}

}
