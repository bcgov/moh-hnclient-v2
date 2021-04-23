package ca.bc.gov.hlth.hnclientv2.handshakeserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hnclientv2.error.ErrorBuilder;
import ca.bc.gov.hlth.hnclientv2.error.MessageUtil;
import io.netty.util.internal.StringUtil;

/**
 * This class connects to listener, does handshake and sends request to ESB.
 * Receives the response from ESB and send it to originator.
 * @author anumeha.srivastava
 *
 */
public class HandshakeServer {

	private static final Logger logger = LoggerFactory.getLogger(HandshakeServer.class);

	private static final int XFER_HANDSHAKE_SEED = 0;
	
	private static final int XFER_HANDSHAKE_SIZE = 8;

	// data indicator
	private static final String DATA_INDICATOR = "DT";
	// message header indicator
	public static final String HEADER_INDICATOR = "MSH";
	// data indicator length
	private static final int DATA_INDICATOR_LENGTH = 2;
	// Segment length for both DT and SI messages
	private static final int SEGMENT_LENGTH = 12;
	// Station Idnetifier length
	private static final int SI_MESSAGE_LENGTH = 44;	

	//Hand shake segment
	private static final String HS_SEGMENT ="HS0000000008"; 
		
	private byte decodeSeed = 0;
	// DT segments to send to POS
	private byte[] dataSegmentOut = new byte[SEGMENT_LENGTH];
	private byte[] dataHL7out;

	private static final String TEN_ZEROS = "0000000000";

	private final ProducerTemplate producer;

	private String hnSecureResponse = "";

	private static final HandshakeUtil util = new HandshakeUtil();

	/** Port to start the Handshake server on */
    private Integer serverSocket;

    /** Time in ms to wait between reads */
    private Integer socketReadSleepTime;

    /** Number of reads to attempt */
    private Integer maxSocketReadTries;

	public HandshakeServer(ProducerTemplate producer, Integer serverSocket, Integer socketReadSleepTime, Integer maxSocketReadTries) {
		this.producer = producer;
		this.serverSocket = serverSocket;
		this.socketReadSleepTime = socketReadSleepTime;
		this.maxSocketReadTries = maxSocketReadTries;
		connectionHandler();
	}

	/**
	 * This function accepts connection from client , calls handshake method
	 * and if handshake is successful, performs HL7 transaction
	 * if Handshake fails then it sends a error response to client
	 */
	public void connectionHandler() {
		final String methodName = "connectionHandler";

		// Run the ServerSocket in it's own Thread so that it's not blocking the rest of the Camel
		Runnable serverTask = new Runnable() {
			/**
			 *
			 */
			@Override
			public void run() {
				ServerSocket mysocket;
				try {
					mysocket = new ServerSocket(serverSocket);
					while (true) {
						Socket connectionSocket = mysocket.accept();

						// TODO (weskubo-cgi) Each connection needs to be handled by a new Thread in order for the server to be Multithreaded
						
						logger.info("{} - Accepting connection attempt from IP Address: {}", methodName,
								connectionSocket.getInetAddress().getCanonicalHostName());

						BufferedInputStream socketInput = new BufferedInputStream(connectionSocket.getInputStream(),
								1000);
						BufferedOutputStream socketOutput = new BufferedOutputStream(
								connectionSocket.getOutputStream());

						logger.info("{}- Started thread to handle HL7Xfer connection on socket: {}", methodName,
								serverSocket);

						String ret_code = xfer_ReceiveHSSegment(socketOutput, socketInput, XFER_HANDSHAKE_SEED);

						if (ret_code.contentEquals(MessageUtil.HNET_RTRN_SUCCESS)) {
							performTransaction(socketInput, socketOutput);

						} else {
							logger.info("{} - Handshake failed: {} {}", methodName, ret_code, System.lineSeparator());
							hnSecureResponse = ErrorBuilder.buildDefaultErrorMessage(ret_code);
							sendResponse(socketOutput, hnSecureResponse);
							// reset decodeseed
							decodeSeed = 0;
						}
					}
				} catch (SocketException e) {
					logger.error(e.getMessage());
				} catch (IOException e) {
					logger.error(e.getMessage());
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}

			}

			/**
			 * Reads client message and sends request to ESB
			 * @param socketInput
			 * @param socketOutput
			 * @throws IOException
			 * @throws InterruptedException
			 */
			private void performTransaction(BufferedInputStream socketInput, BufferedOutputStream socketOutput)
					throws IOException, InterruptedException {
				String headerIn;
				String methodName = "performTransaction";
				String ret_code = MessageUtil.HNET_RTRN_SUCCESS;

				logger.info("{} - Start performing message transaction: {}", methodName, ret_code);
				// read SI segment
				if (socketInput.available() > 0) {
					byte[] message = new byte[SI_MESSAGE_LENGTH];
					socketInput.read(message);
					
					util.unScrambleData(message, decodeSeed);
					logInputData(message);					
					logger.debug("{} - Received from originator {} byte SI Data Block: {}", methodName, message.length,
							ret_code);
					logger.info("{} - Received SI segment: {}", methodName, new String(message, StandardCharsets.UTF_8));
				} else {
					ret_code = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
					logger.debug("{} - Error receiving SI segment from Listener: {}", methodName, ret_code);

				}

				// read dtsegment header
				byte[] dtsegment = new byte[SEGMENT_LENGTH];

				socketInput.read(dtsegment, 0, SEGMENT_LENGTH);
				util.unScrambleData(dtsegment, decodeSeed);
				logInputData(dtsegment);
				int numSocketReadTries = 0;
				headerIn = new String(dtsegment, StandardCharsets.UTF_8);

				while (!headerIn.contains(DATA_INDICATOR)) {
					socketInput.read(dtsegment, 0, SEGMENT_LENGTH);
					headerIn = util.unScrambleData(dtsegment, decodeSeed);

					if (numSocketReadTries < maxSocketReadTries) {
						numSocketReadTries++;
						// Give other packets a chance to arrive and
						// avoid trying to monopolize CPU time..
						Thread.sleep(socketReadSleepTime);
					} else {
						// Give up eventually.
						logger.debug("{} Error receiving DT segment from Listener: {}", methodName, ret_code);
						ret_code = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
						hnSecureResponse = ErrorBuilder
								.buildDefaultErrorMessage(MessageUtil.HL7Error_Msg_ErrorDTHeaderToHNClient);
					}

				}

				// Look for the start of data
				if (headerIn.contains(DATA_INDICATOR)) {
					int messageLength = Integer
							.parseInt(headerIn.substring(DATA_INDICATOR_LENGTH, SEGMENT_LENGTH));

					numSocketReadTries = 0;

					while (socketInput.available() < messageLength) {
						if (numSocketReadTries < maxSocketReadTries) {
							numSocketReadTries++;
							// Give other packets a chance to arrive and
							// avoid trying to monopolize CPU time..
							Thread.sleep(socketReadSleepTime);
						} else {
							// Give up eventually.
							ret_code = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
							logger.debug("{} - Error receiving HL7 message from Listener: {}", methodName, ret_code);

						}
					}

					logger.debug("{} - Received from originator {} byte DT Data Block: {}", methodName, messageLength,
							ret_code);
					// read dtsegment data
					byte[] dtMessage = new byte[messageLength];
					socketInput.read(dtMessage, 0, messageLength);
					util.unScrambleData(dtMessage, decodeSeed);
					logInputData(dtMessage);
					String HL7IN = new String(dtMessage, StandardCharsets.UTF_8);
					int indexOfMSG = HL7IN.indexOf(HEADER_INDICATOR);

					if (indexOfMSG != -1) {
						// Read whatever is after the MSH segment.
						HL7IN = HL7IN.substring(indexOfMSG) + "\r";
						logger.debug("{} - HL7 message recived from POS: {}", methodName, HL7IN);
					}

					// Send received message to server
					logger.debug("{} - HL7 message received from POS: {}", methodName, HL7IN);
					try {
						logger.info("{} - Attempting to send the txn to remote server: {}", methodName, ret_code);
						hnSecureResponse = (String) producer.requestBody(HL7IN);
					} catch (Exception e) {
						logger.error("{} - Error while sending request to ESB  :{}",e.getMessage());
						ret_code = MessageUtil.HNET_RTRN_REMOTETIMEOUT;
						hnSecureResponse = ErrorBuilder
								.buildHTTPErrorMessage(MessageUtil.HL7Error_Msg_ServerUnavailable, null);
					}

				} else {
					logger.debug("{} - Error recieving DT segment: {}", methodName,
							MessageUtil.HL7Error_Msg_ErrorDTHeaderToHNClient);
					hnSecureResponse = ErrorBuilder
							.buildDefaultErrorMessage(MessageUtil.HL7Error_Msg_ErrorDTHeaderToHNClient);
				}

				// Write Response back to POS
				if (StringUtil.isNullOrEmpty(hnSecureResponse)) {
					hnSecureResponse = ErrorBuilder
							.buildDefaultErrorMessage(MessageUtil.HL7Error_Msg_ServerUnavailable);
				}
				sendResponse(socketOutput, hnSecureResponse);

				// sent Response to POS
				logger.info("{} - HL7 transaction is done: {} {}", methodName, ret_code, System.lineSeparator());
				decodeSeed = 0;

			}

			/**
			 * Writes HL7 response back to POS
			 * @param socketOutput
			 * @param hnSecureResponse
			 * @throws IOException
			 */
			private void sendResponse(BufferedOutputStream socketOutput, String hnSecureResponse) throws IOException {

				logger.debug("{} Started writing HL7 reponsec back to POS", methodName);
				String dtSegment = insertHeader(hnSecureResponse);

				dataSegmentOut = dtSegment.substring(0, SEGMENT_LENGTH).getBytes(StandardCharsets.UTF_8);

				dataHL7out = dtSegment.substring(SEGMENT_LENGTH).getBytes(StandardCharsets.UTF_8);
				logger.debug("{} HL7 Response:{}", methodName, new String(dataHL7out));

				// scramble each segment prior to sending it to BufferedOutputStream
				util.scrambleData(dataSegmentOut, decodeSeed);
				socketOutput.write(dataSegmentOut);

				util.scrambleData(dataHL7out, decodeSeed);
				socketOutput.write(dataHL7out);
				socketOutput.flush();

				logger.debug("{} Ending writing HL7 reponsec back to POS", methodName);
			}

			/**
			 * This function logs the input data(SI and DT segment)
			 * @param dtsegment
			 * @return
			 */
			private void logInputData(byte[] dtsegment) {
				if(logger.isDebugEnabled() ) {
					String output1 = new String(dtsegment, StandardCharsets.UTF_8);
					Scanner s1 = new Scanner(output1);
					while (s1.hasNextLine()) {
						logger.debug("{} reading extracted data :{}", methodName, s1.nextLine());
					}
					s1.close();
				}
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
			throws IOException {
		String methodName = "xfer_ReceiveHSSegment";
		logger.debug("{} Received handshake seed to compute challange data : {}", methodName, handshakeSeed);

		byte[] handshakeData = new byte[XFER_HANDSHAKE_SIZE];
		byte[] clientHandshakeData = new byte[XFER_HANDSHAKE_SIZE];
		String retCode = MessageUtil.HNET_RTRN_SUCCESS;
		if (handshakeSeed == null) {
			retCode = MessageUtil.HNET_RTRN_INVALIDPARAMETER;
			logger.debug("{} - Error receiving HS segment:{}", methodName, retCode);
			return retCode;
		}
		/* Create the initial handshake data. **/
		retCode = util.generateHandshakeData(handshakeData);

		/* Now send the Handshake Segment Header. */
		if (retCode.equals(MessageUtil.HNET_RTRN_SUCCESS)) {
			bos.write(HS_SEGMENT.getBytes(), 0, SEGMENT_LENGTH);
			bos.flush();
		}

		/* Now send the actual handshake data */
		if (retCode.equals(MessageUtil.HNET_RTRN_SUCCESS)) {
			bos.write(handshakeData);
			bos.flush();

			logger.debug("{} - Sent {} HS Data Block to Originator :{}", methodName, XFER_HANDSHAKE_SIZE, retCode);
		}

		/*
		 * Now receive and verify the originator's response to the handshake data. This
		 * must be the first segment which the originator sends.
		 */
		if (retCode.equals(MessageUtil.HNET_RTRN_SUCCESS))
			retCode = recvVerifyHandshakeData(ios, handshakeData, clientHandshakeData);

		if (retCode.equals(MessageUtil.HNET_RTRN_SUCCESS))
			decodeSeed = handshakeData[XFER_HANDSHAKE_SIZE - 1];

		logger.debug("{} Completed handshake with return code as : {}", methodName, retCode);
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
		String methodName = "recvVerifyHandshakeData";

		String retCode = MessageUtil.HNET_RTRN_SUCCESS;
		logger.info("{} Computing expected handshake data", methodName);

		// Check parameters.
		if (handShakeData == null) {
			retCode = MessageUtil.HNET_RTRN_INVALIDPARAMETER;
			logger.debug("{} Origninal HandshakeData parameter cannot be NULL : {}", methodName, retCode);
			return retCode;

		} else if (clientHandshakeData == null) {
			retCode = MessageUtil.HNET_RTRN_INVALIDPARAMETER;
			logger.debug("{} ClientHandshakeData parameter cannot be NULL: {}", methodName, retCode);

			return retCode;
		} else {
			byte[] clientHandshakesegment = new byte[SEGMENT_LENGTH];

			// read and verify the handshake header
			ios.read(clientHandshakesegment, 0, SEGMENT_LENGTH);

			if (!util.compareByteArray(clientHandshakesegment, HS_SEGMENT.getBytes())) {
				retCode = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
				logger.debug("{} Handshake header segment are not matched :{}", methodName, retCode);
				return retCode;
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
		String retCode = MessageUtil.HNET_RTRN_SUCCESS;

		String methodName = "verifyHandshakeResponse";
		logger.debug("{} - Received client and original handshake data are {} ,{}", methodName, clientHandshakeData,
				originalHandshakeData);

		// Scramble the original handshake data
		util.scrambleData(originalHandshakeData, decodeSeed);

		// Compare client handshake data and original handshake data
		if (!util.compareByteArray(clientHandshakeData, originalHandshakeData)) {
			retCode = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
			logger.debug("{} - Received handshake data does not match expected data :{}", methodName, retCode);

		} else {
			logger.debug("{} - Received handshake data matched expected data :{}", methodName, retCode);
		}
		return retCode;
	}



	protected String insertHeader(String aMessage) {
		String lengthOfMessage = String.valueOf(aMessage.length());
		aMessage = "DT" + TEN_ZEROS.substring(0, TEN_ZEROS.length() - lengthOfMessage.length()) + lengthOfMessage
				+ aMessage;
		return aMessage;
	}

}
