package com.cgi.hl7v2sendmessage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class HandshakeClient {

	private static Socket netSocket = null;

	private static byte decodeSeed = 0;

	// HandShake Segment
	private static byte handShakeSegment[] = new byte[12];
	private static byte handShakeData[] = new byte[8];

	// define ten cute little 0's contained in the hnclient's heart beat.
	private static final String TEN_ZEROS = "0000000000";

	// DT segments received from HNClient
	private static byte dataSegmentin[] = new byte[12];
	private static byte dataHL7in[];

	// DT segments to send to HNClient
	private static byte dataSegmentout[] = new byte[12];
	private static byte dataHL7out[];

	// Station Idetification Segment
	private static StringBuffer ipAddress = new StringBuffer("                                            "); // 44
																												// spaces

	// data indicator
	private static final String DATA_INDICATOR = "DT";
	// message header indicator
	public static final String HEADER_INDICATOR = "MSH";
	// data indicator length
	private static final int DATA_INDICATOR_LENGTH = 2;
	// length indicator length
	private static final int LENGTH_INDICATOR_LENGTH = 12;

	private static String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
			+ "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n" + "PID||1234567890^^^BC^PH";

	public static void main(String args[]) {
		final int SOCKET_READ_SLEEP_TIME = 100; // milliseconds
		final int MAX_SOCKET_READ_TRIES = 100; // total of 10 seconds
		int numSocketReadTries = 0;
		String headerIn;
		String HL7IN;

		String aResponse = null;

		try {

			System.out.println("Client: " + "Connection Established");
			Socket netSocket = new Socket("127.0.0.1", 5555);

			BufferedInputStream socketInput = new BufferedInputStream(netSocket.getInputStream(), 1000);
			BufferedOutputStream socketOutput = new BufferedOutputStream(netSocket.getOutputStream());

			// Wait for data from HNClient
			while (socketInput.available() < 1 && numSocketReadTries < MAX_SOCKET_READ_TRIES) {
				numSocketReadTries++;
				java.lang.Thread.sleep(SOCKET_READ_SLEEP_TIME);
				System.out.println("Waitng for HNClient");
			}

			// data available for read in BufferedInputStream
			if (socketInput.available() > 0) {
				// Read 12 bytes of HandShake Segment and 8 bytes of HandShake data
				System.out.println("Reading handshake data client side");
				socketInput.read(handShakeSegment, 0, 12);
				socketInput.read(handShakeData, 0, 8);

				// write 12 bytes of HS response to BufferedOutputStream
				socketOutput.write("HS0000000008".getBytes(), 0, 12);
				System.out.println("Wrote HSSegment to listener");

				// write 8 bytes of scrambled HandShake data to BufferedOutputStream
				socketOutput.write(scrambleData(handShakeData), 0, 8);

				// set decodeSeed to last byte of scrambled handShakeData
				// Same decodeSeed be used to unscramble data on listener side
				decodeSeed = handShakeData[7];

				String dtSegment = insertHeader(v2Msg);

				// separate DT Segment from HL7 message (aMessage)
				dataSegmentout = dtSegment.substring(0, 12).getBytes("UTF-8");
				System.out.println("datasegmentout---" + dataSegmentout);
				dataHL7out = dtSegment.substring(12).getBytes("UTF-8");

				// grab the hostname of the computer
				String siHeader = getHostName();
				byte[] siHeaderByte = siHeader.substring(0, 12).getBytes();
				byte[] siHostName = siHeader.substring(12).getBytes();

				System.out.print("dataHL7out: " + new String(dataHL7out));

				// scramble each segment prior to sending it to BufferedOutputStream
				socketOutput.write(scrambleData(siHeaderByte));
				socketOutput.write(scrambleData(siHostName));
				socketOutput.write(scrambleData(dataSegmentout));
				socketOutput.write(scrambleData(dataHL7out));

				// send Buffered OutputStream data to HNClient
				socketOutput.flush();

				// ******************************************************************************

				// Read response
				try {

					// This guarantees us that we have a data header in the result
					socketInput.read(dataSegmentin, 0, 12);

					if (dataSegmentin.length > 0) {
						System.out.println("dataSegmentin---------------"+dataSegmentin);
						// data received from HNClient is in scrambled from and must be unscrambled
						headerIn = unScrambleData(dataSegmentin);
						System.out.println("POS headerIn -------"+headerIn);
						numSocketReadTries = 0;
						while (!headerIn.contains(DATA_INDICATOR)) {
							socketInput.read(dataSegmentin, 0, 12);
							//headerIn = unScrambleData(dataSegmentin);

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
						}

						// Look for the start of data
						if (headerIn.contains(DATA_INDICATOR)) {
							System.out.println(headerIn);
							int messageLength = Integer
									.parseInt(headerIn.substring(DATA_INDICATOR_LENGTH, LENGTH_INDICATOR_LENGTH));

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
							};
							dataHL7in = new byte[messageLength];
							socketInput.read(dataHL7in, 0, messageLength);
							HL7IN = unScrambleData(dataHL7in);
							
							System.out.println("POS HL7in-----------"+HL7IN);

							int indexOfMSG = HL7IN.indexOf(HEADER_INDICATOR);

							if (indexOfMSG != -1) {
								//
								// Read whatever is after the MSH segment.
								aResponse = HL7IN.substring(indexOfMSG) + "\r";
								System.out.println("Response ----"+aResponse);
							} else {
								throw new Exception("Could not find MSG header");
							}
						} else {
							throw new Exception("Could not find DT header");
						}
					} else {
						throw new Exception("Response message is empty");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				// *******************************************************************************

				if (netSocket != null) {
					socketInput.close();
					socketOutput.close();
					netSocket.close();
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * <p>
	 * The new HNClient 4.1.d expects data which it receives to be in scrambled from
	 * This method scrambles incoming data (ex. HL7 message ) using exclusive OR and
	 * a decodeSeed The same decodeSeed is used in unscrambling the data
	 *
	 * @param aByte array of byte
	 * @return array of byte
	 */
	public static byte[] scrambleData(byte[] aByte) {
		aByte[0] ^= decodeSeed;
		for (int x = 1; x < aByte.length; x++) {
			aByte[x] ^= aByte[x - 1];
		}

		return aByte;
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
	 * Insert the method's description here. Creation date: (2/6/01 4:38:50 PM)
	 * (JLee)
	 *
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
	public static String unScrambleData(byte[] scrambleByte) {

		byte prevByte = scrambleByte[0];
		scrambleByte[0] ^= decodeSeed;
		for (int x = 1; x < scrambleByte.length; x++) {
			byte currByte = scrambleByte[x];
			scrambleByte[x] ^= prevByte;
			prevByte = currByte;
		}
		return new String(scrambleByte);
	}

	/**
	 * This method inserts the RAI HL7 message header into the flatwire message.
	 *
	 * <p>
	 * All messages have the same header.
	 * DT000000000000MSH.....<CR>........<CR>......<CR><CR>.
	 *
	 * @return java.lang.String updated message.
	 * @param aMessage java.lang.String the message to be processed.
	 */
	protected static String insertHeader(String aMessage) {
		String lengthOfMessage = String.valueOf(aMessage.length());
		aMessage = "DT" + TEN_ZEROS.substring(0, TEN_ZEROS.length() - lengthOfMessage.length()) + lengthOfMessage
				+ aMessage;
		System.out.println(aMessage);
		return aMessage;
	}

	/**
	 * Insert the method's description here. Creation date: (2/9/01 2:41:43 PM)
	 *
	 * @return byte[]
	 * @throws java.net.UnknownHostException
	 */
	private static String getHostName() throws UnknownHostException {
		try {

			String localAddress = java.net.InetAddress.getLocalHost().toString();
			int slash = localAddress.indexOf("/");
			String machineName = localAddress.substring(0, slash);

			ipAddress.replace(0, 12, "SI0000000032A");

			if (machineName.length() > 16) {
				ipAddress.replace(13, 28, machineName.substring(0, 16));
			} else {
				ipAddress.replace(13, 28, machineName);
			}

			ipAddress.replace(29, 43, "127.0.0.1");

			int len = 45 - ipAddress.length();
			for (int x = 1; x < len; x++) {
				ipAddress.append(" ");
			}

			ipAddress.setLength(44);

		} catch (java.net.UnknownHostException e) {
			throw new UnknownHostException("Local Host IP Address not found");
		}
		System.out.println("ip address--" + ipAddress.toString());
		return ipAddress.toString();
	}

}
