/*
 * Copyright 2017 Killian.Faussart.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.hlth.hl7v2plugin.hl7xfer;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Killian.Faussart
 */
public class HL7XferMessageSender {
	private static final Logger logger = LoggerFactory.getLogger(HL7XferMessageSender.class);
	
	//properties
	private static final String HNCLIENT_PORT = "hnclientPort";
	private static final String HNCLIENT_ADDRESS = "hnclientAddress";
	private static final String HNCLIENT_TIMEOUT = "hnclientTimeout";
	//Listen port for HNCLIENT.EXE
	private int hnclientPort = 0;
	//Assume HNCLIENT.EXE runs on the local machine
	private String hnclientAddress = null;
	//timeout value for socket reads
	private int hnclientTimeout =0;
	//data indicator
	private static final String DATA_INDICATOR = "DT";
	//message header indicator
	public static final String HEADER_INDICATOR = "MSH";
	//data indicator length
	private static final int DATA_INDICATOR_LENGTH = 2;
	//length indicator length
	private static final int LENGTH_INDICATOR_LENGTH = 12;
	//define ten cute little 0's contained in the hnclient's heart beat.
	private static final String TEN_ZEROS = "0000000000";
	//Transaction socket
	private java.net.Socket netSocket = null;
	//Net address of the client
	private java.net.InetAddress netAddress = null;
	//Socket readtimeout indicator
	private boolean socketIsBroken = false;

	private byte baseSeed =0;
	private byte decodeSeed =0;

	// HandShake Segment
	private byte handShakeSegment[] = new byte [12];
	private byte handShakeData[] = new byte[8];


	private byte stationIdSegment[] = new byte [12];
	private byte stationIdData[] = new byte [32];

	// DT segments received from HNClient
	private byte dataSegmentin[] = new byte [12];
	private byte dataHL7in[];

	// DT segments to send to HNClient
	private byte dataSegmentout[] = new byte [12];
	private byte dataHL7out[];


	// Station Idetification Segment

	private StringBuffer ipAddress = new StringBuffer("                                            "); // 44 spaces

	/**
	 * Insert the method's description here.
	 * Creation date: (1/9/2002 11:24:00 AM)
	 * @throws HL7XferException 
	 */
	//Overloaded constructor. Connect to aServerPort as opposed to default port.
	//This constructor required for Teleplan and added by Stephen Laws on 08-08-2001
	public HL7XferMessageSender(int port, int timeout, String host) throws HL7XferException {
		hnclientPort = port;
		hnclientTimeout = timeout;
		hnclientAddress = host;
		try {
			netAddress = java.net.InetAddress.getByName(hnclientAddress);
		} catch (UnknownHostException uhe) {
			logger.error("Error getting InetAddress by Name", uhe);
			throw new HL7XferException(uhe.getMessage());
		}
	}
	
	/**
	 * This method overrides the finalize method.
	 * <p>
	 * It is a clean up method closing the socket if it is still open and destroying
	 * the timer thread if it is still running.
	 */
	protected void finalize() throws Throwable {
		if (netSocket != null) {
			try {
				netSocket.close();
				netSocket = null;
			} catch (java.io.IOException e) {
			}
		}
		super.finalize();
	}
	
	/**
	 * Insert the method's description here. Creation date: (2/9/01 2:41:43 PM)
	 * 
	 * @return byte[]
	 * @throws java.net.UnknownHostException
	 */
	public String getHostName() throws UnknownHostException {
		try {

			String localAddress = java.net.InetAddress.getLocalHost().toString();
			int slash = localAddress.indexOf("/");
			String machineName = localAddress.substring(0, slash);
			String ip = localAddress.substring(slash + 1);

			ipAddress.replace(0, 12, "SI0000000032A");

			if (machineName.length() > 16)
				ipAddress.replace(13, 28, machineName.substring(0, 16));
			else
				ipAddress.replace(13, 28, machineName);

			ipAddress.replace(29, 43, ip);

			int len = 45 - ipAddress.length();
			for (int x = 1; x < len; x++)
				ipAddress.append(" ");

			ipAddress.setLength(44);

		} catch (java.net.UnknownHostException e) {
			throw new UnknownHostException(new String("Local Host IP Address not found"));
		}
		return ipAddress.toString();
	}
	
	/**
	 * This method inserts the RAI HL7 message header into the flatwire message.
	 * <p>
	 * All messages have the same header.
	 * DT000000000000MSH.....<CR>........<CR>......<CR><CR>.
	 * 
	 * @return java.lang.String updated message.
	 * @param aMessage java.lang.String the message to be processed.
	 */
	protected String insertHeader(String aMessage) {
		String lengthOfMessage = String.valueOf(aMessage.length());
		aMessage = "DT" + this.TEN_ZEROS.substring(0, this.TEN_ZEROS.length() - lengthOfMessage.length()) + lengthOfMessage + aMessage;
		return aMessage;
	}

	/**
	 * Insert the method's description here. Creation date: (2/6/01 4:20:18 PM)
	 * (JLee)
	 *
	 * The new HNClient 4.1.d expects data which it receives to be in scrambled from
	 * This method scrambles incoming data (ex. HL7 message ) using exclusive OR and
	 * a decodeSeed The same decodeSeed is used in unscrambling the data
	 *
	 * @param aByte array of byte
	 * @return array of byte
	 */
	protected byte[] scrambleData(byte[] aByte) {

		aByte[0] ^= decodeSeed;
		for (int x = 1; x < aByte.length; x++) {
			aByte[x] ^= aByte[x - 1];
		}
		return aByte;
	}

	/**
	* This method sends an HL7 message to the hnclient application.
	* <p>
	* Opens communications to hnclient, starts the timeout thread, reports any communications errors.
	* @return java.lang.String, the flatwire to be sent
	* @param aMessage java.lang.String, the flatwire to be sent.
	*/
	public String send(String aMessage) throws HL7XferException
	{
		final int SOCKET_READ_SLEEP_TIME = 100;  // milliseconds
		final int MAX_SOCKET_READ_TRIES  = 100;  // total of 10 seconds
		String headerIn;
		String HL7IN;
		int numSocketReadTries = 0;
		
		String aResponse = null;
		
		try {	
	        netSocket = new java.net.Socket(netAddress, hnclientPort);
			java.io.BufferedInputStream socketInput = new java.io.BufferedInputStream(netSocket.getInputStream(),1000);
			java.io.BufferedOutputStream socketOutput = new java.io.BufferedOutputStream(netSocket.getOutputStream());
			
			// wait for data from HNClient	
			while ( socketInput.available() < 1 && numSocketReadTries < MAX_SOCKET_READ_TRIES ) {
				numSocketReadTries++;
			    java.lang.Thread.sleep( SOCKET_READ_SLEEP_TIME );
			} 
	
	 		// data available for read in BufferedInputStream
			if (socketInput.available() > 0) {
				// read 12 bytes of HandShake Segment and 8 bytes of HandShake data
				socketInput.read(handShakeSegment,0,12);
				socketInput.read(handShakeData,0,8);
			 
				// write 12 bytes of HS response to BufferedOutputStream
				socketOutput.write("HS0000000008".getBytes());
	
				// write 8 bytes of scrambled HandShake data to BufferedOutputStream
				socketOutput.write(scrambleData(handShakeData));
	
				// set decodeSeed to last byte of scrambled handShakeData
				decodeSeed = handShakeData[7];
				String dtSegment = insertHeader(aMessage);
	
				// separate DT Segment from HL7 message (aMessage)
				dataSegmentout = dtSegment.substring(0,12).getBytes();	
				dataHL7out = dtSegment.substring(12).getBytes();
				//logger.debug("HL7: "+aMessage);
	
				// grab the hostname of the computer
				String siHeader = getHostName();
				byte[] siHeaderByte = siHeader.substring(0,12).getBytes();
				byte[] siHostName = siHeader.substring(12).getBytes();
				
				// scramble each segment prior to sending it to BufferedOutputStream
				socketOutput.write(scrambleData(siHeaderByte));
				socketOutput.write(scrambleData(siHostName));
				socketOutput.write(scrambleData(dataSegmentout));
				socketOutput.write(scrambleData(dataHL7out));
	
				// send Buffered OutputStream data to HNClient
				socketOutput.flush();
				
				}
				//Read response
				try {
				//Restart timer
				setBrokenSocket(false);
	
				//This guarantees us that we have a data header in the result
	
				socketInput.read(dataSegmentin,0,12);
	   			
				if ( dataSegmentin.length >0) {
	
					// data received from HNClient is in scrambled from and must be unscrambled
					headerIn =unScrambleData(dataSegmentin);
					//logger.debug(headerIn);
	
					numSocketReadTries = 0;
					while ( headerIn.indexOf(DATA_INDICATOR) == -1 )
						{	
							socketInput.read(dataSegmentin,0,12);
							headerIn =unScrambleData(dataSegmentin);
							//logger.debug(headerIn);
	
	 					  if ( numSocketReadTries < MAX_SOCKET_READ_TRIES )
	 					     {
								numSocketReadTries++;
								//
								//  Give other packets a chance to arrive and
								//  avoid trying to monopolize CPU time..
								//
							    java.lang.Thread.sleep( SOCKET_READ_SLEEP_TIME );
							  }
						  else {
		  					//
								//  Give up eventually.
								//
							    throw new HL7XferException(new String("Could not find DT Response."));
						      }
						 }
					
					//Look for the start of data
	
					if (headerIn.indexOf(DATA_INDICATOR) != -1)
					 {
						int messageLength = Integer.parseInt(headerIn.substring(DATA_INDICATOR_LENGTH, LENGTH_INDICATOR_LENGTH));
						//logger.debug("Length of Message("+messageLength+")");
						
						numSocketReadTries = 0;
						
						while ( socketInput.available() < messageLength)
							{
							  if ( numSocketReadTries < MAX_SOCKET_READ_TRIES ) {
								numSocketReadTries++;
								//
								//  Give other packets a chance to arrive and
								//  avoid trying to monopolize CPU time..
								//
							    java.lang.Thread.sleep( SOCKET_READ_SLEEP_TIME );
							  }
							  else {
								//
								//  Give up eventually.
								//
							    throw new HL7XferException(new String("Could not find entire message."));
							  }
	
						   };
						dataHL7in = new byte[messageLength];
						socketInput.read(dataHL7in,0,messageLength);
						HL7IN =  unScrambleData(dataHL7in);
						//logger.debug(dataHL7in);
	
						int indexOfMSG = HL7IN.indexOf(HEADER_INDICATOR);
	
						if (indexOfMSG != -1)
						{
							//
							//  Read whatever is after the MSH segment.
							aResponse = HL7IN.substring(indexOfMSG)+"\r";
						}
						else
							throw new HL7XferException(new String("Could not find MSG header"));
					}
					else
						throw new HL7XferException(new String("Could not find DT header"));
				}
				else
					throw new HL7XferException(new String("Response message is empty"));
			}
			catch (Exception e)
			{
				throw new HL7XferException(new String("Could not read response message"));
			}
			if (netSocket != null) {
				socketInput.close();
				socketOutput.close();
				netSocket.close();
			}
		} catch (java.net.UnknownHostException e) {
			throw new HL7XferException(new String("Could not locate host "));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new HL7XferException(new String("Error in send method"));
		}
		//logger.debug("HL7 Return: "+aResponse);
		return aResponse;
	}
	
	/**
	 * This method changes the value of socketIsBroken attribute.
	 * <p>
	 * This method does not need to be synchronized since the main thread accesses
	 * the attribute prior to instantiating the timer thread.
	 * 
	 * @param value boolean the new value for socketIsBroken.
	 */
	protected void setBrokenSocket(boolean value) {
		socketIsBroken = value;
	}
	
	/**
	 * Insert the method's description here. Creation date: (2/6/01 4:38:50 PM)
	 * (JLee)
	 *
	 * The new HNClient 4.1.d sends data in a scrambled form after the initial
	 * HandShaking data (12 bytes), and random 8 bytes (total 20 bytes) are sent
	 * This method unscrambles the data using exclusive OR and a decode seed The
	 * decode seed is derived from the random set of 8 bytes (it is also used in
	 * data scrambling)
	 * 
	 * @param scrambleByte byte[]
	 */
	public String unScrambleData(byte[] scrambleByte) {

		byte prevByte = scrambleByte[0];
		scrambleByte[0] ^= decodeSeed;
		for (int x = 1; x < scrambleByte.length; x++) {
			byte currByte = scrambleByte[x];
			scrambleByte[x] ^= prevByte;
			prevByte = currByte;
		}
		return new String(scrambleByte);
	}
}
