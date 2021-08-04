
package com.cgi.bathl7;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to establish a socket connection and perform
 * transaction with HNClient using hl7xfer protocol. Sends and receives HL7
 * messages to/from HNClient
 * 
 * @author anumeha.srivastava
 *
 */
public class ConnectionHandler {

	private Socket netSocket = null;

	private static final String DEFAULT_IP = "127.0.0.1";

	private static final String DEFAULT_PORT = "5555";
	
	private static final Logger logger = LoggerFactory.getLogger(BatHL7Processor.class);

	/**
	 * This method establishes a socket connection for each request with client on
	 * given port As per the hlxfer protocol the handshake is performed for each
	 * request.
	 * Sends HL7 request messages to HNClient and receives hl7 response
	 * messages.
	 * @param v2Msg
	 * @return response, received from downstream.
	 */
	protected String socketConnection(String v2Msg, String clientAddress,String port) throws Exception {

		int numSocketReadTries = 0;
		String transactionOut = null;
		String ip = null;
		String clientPort = null;
		BufferedInputStream socketInput = null;
		BufferedOutputStream socketOutput = null;

		try {
			ip = (StringUtils.isBlank(clientAddress)) ? DEFAULT_IP : clientAddress;
			clientPort = (StringUtils.isBlank(port)) ? DEFAULT_PORT : port;

			netSocket = new Socket(ip, Integer.valueOf(clientPort));
			socketInput = new BufferedInputStream(netSocket.getInputStream(), 1000);
			socketOutput = new BufferedOutputStream(netSocket.getOutputStream());

			// Wait for data from HNClient
			while (socketInput.available() < 1 && numSocketReadTries < HL7XferTransaction.MAX_SOCKET_READ_TRIES) {
				numSocketReadTries++;
				java.lang.Thread.sleep(HL7XferTransaction.SOCKET_READ_SLEEP_TIME);
			}
			// data available for read in BufferedInputStream
			if (socketInput.available() > 0) {
				HL7XferTransaction conn = new HL7XferTransaction();
				conn.sendRequest(socketInput, socketOutput, v2Msg);
				// Read response
				transactionOut = conn.readResponse(socketInput);
				return transactionOut;
			}

		} catch (NumberFormatException nfe) {
			throw nfe;
		}
		catch (UnknownHostException e) {
			logger.error("Unknown host");
			//System.exit(0);
		} catch (SocketException se) {
			logger.error("Unable to connect HNClient.");
		}
		finally {
			if (socketInput != null) {
				try {
					socketInput.close();
				} catch (IOException e) {
					logger.error("Error while connecting to client");
				}
			}
			if (socketOutput != null) {
				try {
					socketOutput.close();
				} catch (IOException e) {
					logger.error("Error while connecting to client");
				}
			}
		}
		return transactionOut;

	}

}
