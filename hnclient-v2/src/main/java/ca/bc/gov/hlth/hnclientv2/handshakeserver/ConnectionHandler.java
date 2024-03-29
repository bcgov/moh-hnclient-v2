package ca.bc.gov.hlth.hnclientv2.handshakeserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hnclientv2.error.ErrorBuilder;
import ca.bc.gov.hlth.hnclientv2.error.MessageUtil;
import ca.bc.gov.hlth.hncommon.util.LoggingUtil;
import io.netty.util.internal.StringUtil;

public class ConnectionHandler implements Callable<Void> {
	private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

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
	
	// Station Identifier length
	private static final int SI_MESSAGE_LENGTH = 44;	
	
	// Hand shake segment
	private static final String HS_SEGMENT ="HS0000000008"; 
		
	// Header padding
	private static final String TEN_ZEROS = "0000000000";
		
	// DT segments to send to POS
	private byte[] dataSegmentOut = new byte[SEGMENT_LENGTH];
	private byte[] dataHL7out;

	private byte decodeSeed = 0;
	
	private String hnSecureResponse = "";
	
    /** Time in ms to wait between reads */
    private Integer socketReadSleepTime;

    /** Number of reads to attempt */
    private Integer maxSocketReadTries;
   
	private final Socket socket;
	
	private String transactionId;
	
	private final ProducerTemplate producer;
	
	public static final String HTTP_REQUEST_ID_HEADER = "X-Request-Id";
	
	public ConnectionHandler(ProducerTemplate producer, Socket socket, Integer socketReadSleepTime, Integer maxSocketReadTries, String requestId) {
		this.producer = producer;
		this.socket = socket;
		this.socketReadSleepTime = socketReadSleepTime;
		this.maxSocketReadTries = maxSocketReadTries;
		this.transactionId = requestId;
		
	}

	@Override
	public Void call() throws Exception {	
		String methodName = LoggingUtil.getMethodName();
		InputStream socketInput = null;
		OutputStream socketOutput = null;
		String ret_code = null;
		try {
			logger.info("{} - TransactionId: {}. 2 -  Handling connection from IP Address: {}", methodName, transactionId,
					socket.getInetAddress().getHostAddress());
			
			socketInput = new BufferedInputStream(socket.getInputStream());
            socketOutput = new BufferedOutputStream(socket.getOutputStream());

			ret_code = xfer_ReceiveHSSegment(socketOutput, socketInput, XFER_HANDSHAKE_SEED);

			if (ret_code.contentEquals(MessageUtil.HNET_RTRN_SUCCESS)) {
				performTransaction(socketInput, socketOutput);
			} else {
				logger.warn("{} - TransactionId: {} Handshake failed: {} {}", methodName, transactionId,  ret_code, System.lineSeparator());
				hnSecureResponse = ErrorBuilder.buildErrorMessage(ret_code);
				sendResponse(socketOutput, hnSecureResponse);
				// reset decodeseed
				decodeSeed = 0;
			}			
		} catch (Exception e) {
			logger.error("{} - TransactionId: {} Message processing failed: {} {}", methodName, transactionId,  ret_code, System.lineSeparator());							
			
			// Depending on when the handshake process fails the ret_code might actually return HNET_RTRN_SUCCESS
			// If this is the case use a HNET_RTRN_INVALIDFORMATERROR instead as it is likely a formatting error
			if (ret_code.contentEquals(MessageUtil.HNET_RTRN_SUCCESS)) {
				ret_code = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
			}
			hnSecureResponse = ErrorBuilder.buildErrorMessage(ret_code);
			try {
				sendResponse(socketOutput, hnSecureResponse);
			} catch (IOException ioe) {
				logger.error("{} - TransactionId: {}", methodName, transactionId, ioe.getMessage());
			}
			// reset decodeseed
			decodeSeed = 0;
			logger.error("{} - TransactionId: {}", methodName, transactionId,  e.getMessage());
		} finally {
			if (socketInput != null) {
				try {
					socketInput.close();
				} catch (IOException e) {
					logger.error("{} - TransactionId: {}", methodName, transactionId,  e.getMessage());
				}
			}
			if (socketOutput != null) {
				try {
					socketOutput.close();
				} catch (IOException e) {
					logger.error("{} - TransactionId: {}", methodName, transactionId, e.getMessage());
				}
			}
		}

		return null;
	}
	
	private void sendResponse(OutputStream socketOutput, String hnSecureResponse) throws IOException {
		String methodName = LoggingUtil.getMethodName();

		logger.debug("{} - TransactionId: {}  Started writing HL7 reponsec back to POS", methodName, transactionId );
		String dtSegment = insertHeader(hnSecureResponse);

		dataSegmentOut = dtSegment.substring(0, SEGMENT_LENGTH).getBytes(StandardCharsets.UTF_8);

		dataHL7out = dtSegment.substring(SEGMENT_LENGTH).getBytes(StandardCharsets.UTF_8);
		logger.debug("{} - TransactionId: {} HL7 Response:{}", methodName, transactionId, new String(dataHL7out));

		// scramble each segment prior to sending it to BufferedOutputStream
		HandshakeUtil.scrambleData(dataSegmentOut, decodeSeed);
		socketOutput.write(dataSegmentOut);

		HandshakeUtil.scrambleData(dataHL7out, decodeSeed);
		socketOutput.write(dataHL7out);
		socketOutput.flush();

		logger.debug("{} - TransactionId: {} Ending writing HL7 reponsec back to POS", methodName, transactionId);
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
				logger.debug("{} - TransactionId: {} reading extracted data :{}", LoggingUtil.getMethodName(), transactionId, s1.nextLine());
			}
			s1.close();
		}
	}
	
	/**
	 * Reads client message and sends request to ESB
	 * @param socketInput
	 * @param socketOutput
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void performTransaction(InputStream socketInput, OutputStream socketOutput)
			throws IOException, InterruptedException {
		String headerIn;
		String methodName = LoggingUtil.getMethodName();
		String ret_code = MessageUtil.HNET_RTRN_SUCCESS;

		logger.info("{} - TransactionId: {} Start performing message transaction: {}", methodName, transactionId,  ret_code);
		// read SI segment
		
		logger.info("{} - TransactionId: {} Start reading SI segment: {}", methodName, transactionId,  ret_code);
		byte[] sisegment = new byte[SI_MESSAGE_LENGTH];
		socketInput.readNBytes(sisegment, 0, SI_MESSAGE_LENGTH);			
		HandshakeUtil.unScrambleData(sisegment, decodeSeed);
		logInputData(sisegment);					
		logger.debug("{} - TransactionId: {}  Received from originator {} byte SI Data Block: {}", methodName, transactionId,  sisegment.length,
					ret_code);
		logger.debug("{} - TransactionId: {}  Received SI segment: {}", methodName, transactionId,  new String(sisegment, StandardCharsets.UTF_8));
		
		// read dtsegment header
		logger.debug("{} - TransactionId: {} Start reading DT segment: {}", methodName, transactionId,  ret_code);
		byte[] dtsegment = new byte[SEGMENT_LENGTH];

		socketInput.readNBytes(dtsegment, 0, SEGMENT_LENGTH);
		logInputData(dtsegment);
		HandshakeUtil.unScrambleData(dtsegment, decodeSeed);
		logInputData(dtsegment);
		int numSocketReadTries = 0;
		headerIn = new String(dtsegment, StandardCharsets.UTF_8);

		while (!headerIn.contains(DATA_INDICATOR)) {
			socketInput.readNBytes(dtsegment, 0, SEGMENT_LENGTH);
			headerIn = HandshakeUtil.unScrambleData(dtsegment, decodeSeed);
			logger.debug("{} - TransactionId: {}  Data recieved is: {} - {}", methodName, transactionId, numSocketReadTries, headerIn);
			
			if (numSocketReadTries < maxSocketReadTries) {
				numSocketReadTries++;
				// Give other packets a chance to arrive and
				// avoid trying to monopolize CPU time..
				Thread.sleep(socketReadSleepTime);
			} else {
				// Give up eventually.
				logger.debug("{} - TransactionId: {} Error receiving DT segment from Listener: {}", methodName, transactionId, ret_code);
				ret_code = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
				hnSecureResponse = ErrorBuilder
						.buildErrorMessage(MessageUtil.HL7_ERROR_MSG_ERROR_DT_HEADER_TO_HNCLIENT);
				break;
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
					logger.debug("{} - TransactionId:{} Error receiving HL7 message from Listener: {}", methodName, transactionId,  ret_code);

				}
			}

			logger.debug("{} - TransactionId:{} Received from originator {} byte DT Data Block: {}", methodName, transactionId,  messageLength,
					ret_code);
			// read dtsegment data
			byte[] dtMessage = new byte[messageLength];
			socketInput.readNBytes(dtMessage, 0, messageLength);
			HandshakeUtil.unScrambleData(dtMessage, decodeSeed);
			logInputData(dtMessage);
			String HL7IN = new String(dtMessage, StandardCharsets.UTF_8);
			int indexOfMSG = HL7IN.indexOf(HEADER_INDICATOR);

			if (indexOfMSG != -1) {
				// Read whatever is after the MSH segment.
				HL7IN = HL7IN.substring(indexOfMSG) + "\r";
				logger.debug("{} - TransactionId:{} HL7 message recived from POS: {}", methodName, transactionId, HL7IN);
			}

			// Send received message to server
			logger.debug("{} - TransactionId:{} HL7 message received from POS: {}", methodName, transactionId, HL7IN);
			try {
				logger.info("{} - TransactionId:{} Attempting to send the txn to remote server: {}", methodName, transactionId, ret_code);
				hnSecureResponse = (String) producer.requestBodyAndHeader(HL7IN, HTTP_REQUEST_ID_HEADER, transactionId);
			} catch (Exception e) {
				logger.error("{} - TransactionId:{} Error while sending request to ESB  :{}", methodName, transactionId,e.getMessage());
				ret_code = MessageUtil.HNET_RTRN_REMOTETIMEOUT;
				hnSecureResponse = ErrorBuilder
						.buildErrorMessage(MessageUtil.HL7_ERROR_MSG_SERVER_UNAVAILABLE, null);
			}

		} else {
			logger.debug("{} - TransactionId:{} Error recieving DT segment: {}", methodName, transactionId,
					MessageUtil.HL7_ERROR_MSG_ERROR_DT_HEADER_TO_HNCLIENT);
			hnSecureResponse = ErrorBuilder
					.buildErrorMessage("", MessageUtil.HL7_ERROR_MSG_ERROR_DT_HEADER_TO_HNCLIENT);
		}

		// Write Response back to POS
		if (StringUtil.isNullOrEmpty(hnSecureResponse)) {
			hnSecureResponse = ErrorBuilder
					.buildErrorMessage("", MessageUtil.HL7_ERROR_MSG_SERVER_UNAVAILABLE);
		}
		sendResponse(socketOutput, hnSecureResponse);

		// sent Response to POS
		logger.info("{} - TransactionId: {} HL7 transaction is done: {} {}", methodName, transactionId, ret_code, System.lineSeparator());
		decodeSeed = 0;

	}

	/*----------------------------------------------------------------------------
	 *This function computes the original handshake challenge data for the 
	 * listener and sends this back to the originator. It then waits for the
	 * originator's response, and when the response is received it is compared
	 * with what is expected. If a match occurs, the computed handshake seed 
	 * is returned
	 *@return success/failure message
	 */

	protected String xfer_ReceiveHSSegment(OutputStream bos, InputStream ios, Integer handshakeSeed)
			throws IOException {
		logger.debug("{} - TransactionId: {}. 3. Received handshake seed to compute challenge data : {}", LoggingUtil.getMethodName(), transactionId, handshakeSeed);

		byte[] handshakeData = new byte[XFER_HANDSHAKE_SIZE];
		byte[] clientHandshakeData = new byte[XFER_HANDSHAKE_SIZE];
		String retCode = MessageUtil.HNET_RTRN_SUCCESS;
		if (handshakeSeed == null) {
			retCode = MessageUtil.HNET_RTRN_INVALIDPARAMETER;
			logger.debug("{} - Error receiving HS segment:{}", LoggingUtil.getMethodName(), retCode);
			return retCode;
		}
		/* Create the initial handshake data. **/
		retCode = HandshakeUtil.generateHandshakeData(handshakeData);

		/* Now send the Handshake Segment Header. */
		if (retCode.equals(MessageUtil.HNET_RTRN_SUCCESS)) {
			bos.write(HS_SEGMENT.getBytes(), 0, SEGMENT_LENGTH);
			bos.flush();

			logger.debug("{} - TransactionId: {}. 4. Sent HS Header Block to Originator : {} ({})", LoggingUtil.getMethodName(), transactionId, HS_SEGMENT.getBytes(), HS_SEGMENT);
		}

		/* Now send the actual handshake data */
		if (retCode.equals(MessageUtil.HNET_RTRN_SUCCESS)) {
			bos.write(handshakeData);
			bos.flush();

			logger.debug("{} - TransactionId: {}. 5. Sent HS Data Block to Originator :{}", LoggingUtil.getMethodName(), transactionId, handshakeData);
		}

		/*
		 * Now receive and verify the originator's response to the handshake data. This
		 * must be the first segment which the originator sends.
		 */
		if (retCode.equals(MessageUtil.HNET_RTRN_SUCCESS)) {
			retCode = recvVerifyHandshakeData(ios, handshakeData, clientHandshakeData);
		}

		if (retCode.equals(MessageUtil.HNET_RTRN_SUCCESS))
			decodeSeed = handshakeData[XFER_HANDSHAKE_SIZE - 1];

		logger.debug("{} - TransactionId: {}. 10. Completed handshake with return code as : {}", LoggingUtil.getMethodName(), transactionId, retCode);
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
	protected String recvVerifyHandshakeData(InputStream ios, byte[] handShakeData, byte[] clientHandshakeData)
			throws IOException {
		String methodName = LoggingUtil.getMethodName();

		String retCode = MessageUtil.HNET_RTRN_SUCCESS;
		logger.trace("{} - TransactionId: {} Computing expected handshake data", methodName, transactionId);

		// Check parameters.
		if (handShakeData == null) {
			retCode = MessageUtil.HNET_RTRN_INVALIDPARAMETER;
			logger.debug("{} - TransactionId:{} Original HandshakeData parameter cannot be NULL : {}", methodName, transactionId, retCode);
			return retCode;

		} else if (clientHandshakeData == null) {
			retCode = MessageUtil.HNET_RTRN_INVALIDPARAMETER;
			logger.debug("{} - TransactionId: {} ClientHandshakeData parameter cannot be NULL: {}", methodName, transactionId, retCode);

			return retCode;
		} else {
			byte[] clientHandshakesegment = new byte[SEGMENT_LENGTH];

			// read and verify the handshake header
			ios.readNBytes(clientHandshakesegment, 0, SEGMENT_LENGTH);
			
			logger.debug("{} - TransactionId: {} 6. Reading client HS Header block {} ({})", methodName, transactionId, clientHandshakesegment,  new String(clientHandshakesegment, StandardCharsets.UTF_8));

			if (!HandshakeUtil.compareByteArray(clientHandshakesegment, HS_SEGMENT.getBytes())) {
				retCode = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
				logger.error("{} - TransactionId: {}. Handshake header segment are not matched :{}", methodName, transactionId, retCode);
				return retCode;
			}

			// read and verify the handshake data
			ios.readNBytes(clientHandshakeData, 0, XFER_HANDSHAKE_SIZE);
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

		String methodName = LoggingUtil.getMethodName();
		logger.debug("{} - TransactionId: {}. 7. Received client and original handshake data are {} ,{}", methodName, transactionId, clientHandshakeData,
				originalHandshakeData);

		// Scramble the original handshake data
		HandshakeUtil.scrambleData(originalHandshakeData, decodeSeed);
		logger.debug("{} - TransactionId: {}. 8. Scrambled handshake data: {}", methodName, transactionId, originalHandshakeData);

		// Compare client handshake data and original handshake data
		if (!HandshakeUtil.compareByteArray(clientHandshakeData, originalHandshakeData)) {
			retCode = MessageUtil.HNET_RTRN_INVALIDFORMATERROR;
			logger.error("{} - TransactionId: {}. 9. Received handshake data does not match expected data :{}", methodName, transactionId, retCode);

		} else {
			logger.debug("{} - TransactionId: {}. 9. Received handshake data matched expected data :{}", methodName, transactionId, retCode);
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
