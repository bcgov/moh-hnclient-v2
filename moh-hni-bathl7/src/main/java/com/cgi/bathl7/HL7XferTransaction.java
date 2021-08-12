package com.cgi.bathl7;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

public class HL7XferTransaction {

	private byte decodeSeed = 0;

	// HandShake Segment
	private byte handShakeSegment[] = new byte[12];
	private byte handShakeData[] = new byte[8];

	// define ten cute little 0's contained in the hnclient's heart beat.
	private static final String TEN_ZEROS = "0000000000";

	// DT segments received from HNClient
	private byte dataSegmentin[] = new byte[12];
	private byte dataHL7in[];

	// DT segments to send to HNClient
	private byte dataSegmentout[] = new byte[12];
	private byte dataHL7out[];

	// Station Idetification Segment
	private StringBuffer ipAddress = new StringBuffer("                                            "); // 44 spaces

	// data indicator
	private static final String DATA_INDICATOR = "DT";
	// message header indicator
	public static final String HEADER_INDICATOR = "MSH";
	// data indicator length
	private static final int DATA_INDICATOR_LENGTH = 2;
	// length indicator length
	private static final int LENGTH_INDICATOR_LENGTH = 12;

	public static final int SOCKET_READ_SLEEP_TIME = 100; // milliseconds
	public static final int MAX_SOCKET_READ_TRIES = 100; // total of 10 seconds

	public static int numSocketReadTries = 0;

	/**
	 * This method performs a handshake using hl7xfer protocol with server and sends
	 * request.
	 * @param socketInput
	 * @param socketOutput
	 * @param v2Msg the preformatted HL7v2 message
	 * @throws IOException
	 */
	public void sendRequest(BufferedInputStream socketInput, BufferedOutputStream socketOutput, String v2Msg)
			throws IOException {
		socketInput.read(handShakeSegment, 0, 12);
		socketInput.read(handShakeData, 0, 8);

		// write 12 bytes of HS response to BufferedOutputStream
		socketOutput.write("HS0000000008".getBytes(), 0, 12);

		// write 8 bytes of scrambled HandShake data to BufferedOutputStream
		socketOutput.write(scrambleData(handShakeData), 0, 8);

		// set decodeSeed to last byte of scrambled handShakeData
		// Same decodeSeed be used to unscramble data on listener side
		decodeSeed = handShakeData[7];

		String dtSegment = insertHeader(v2Msg);

		// separate DT Segment from HL7 message (aMessage)
		dataSegmentout = dtSegment.substring(0, 12).getBytes("UTF-8");

		dataHL7out = dtSegment.substring(12).getBytes("UTF-8");

		// grab the hostname of the computer
		String siHeader = getHostName();
		byte[] siHeaderByte = siHeader.substring(0, 12).getBytes();
		byte[] siHostName = siHeader.substring(12).getBytes();

		// scramble each segment prior to sending it to BufferedOutputStream
		socketOutput.write(scrambleData(siHeaderByte));
		socketOutput.write(scrambleData(siHostName));
		socketOutput.write(scrambleData(dataSegmentout));
		socketOutput.write(scrambleData(dataHL7out));

		// send Buffered OutputStream data to HNClient
		socketOutput.flush();
	}

	/**
	 * Reads response from server using hlxfer protocol
	 * @param socketInput
	 */
	protected String readResponse(BufferedInputStream socketInput) {
		try {
			// This guarantees us that we have a data header in the result
			socketInput.read(dataSegmentin, 0, 12);

			if (dataSegmentin.length > 0) {
				// data received from HNClient is in scrambled from and must be unscrambled
				String headerIn = unScrambleData(dataSegmentin);
				numSocketReadTries = 0;

				while (!headerIn.contains(DATA_INDICATOR)) {
					socketInput.read(dataSegmentin, 0, 12);

					if (numSocketReadTries < MAX_SOCKET_READ_TRIES) {
						numSocketReadTries++;
						// Give other packets a chance to arrive and
						// avoid trying to monopolize CPU time..
						java.lang.Thread.sleep(SOCKET_READ_SLEEP_TIME);
					} else {
						// Give up eventually.
						throw new Exception("Could not find DT Response.");
					}
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
							java.lang.Thread.sleep(SOCKET_READ_SLEEP_TIME);
						} else {
							// Give up eventually.
							throw new Exception("Could not find entire message.");
						}
					}
					;
					dataHL7in = new byte[messageLength];
					socketInput.read(dataHL7in, 0, messageLength);
					String hl7in = unScrambleData(dataHL7in);

					int indexOfMSG = hl7in.indexOf(HEADER_INDICATOR);

					if (indexOfMSG != -1) {
						//
						// Read whatever is after the MSH segment.
						String aResponse = hl7in.substring(indexOfMSG) + "\r";
						decodeSeed = 0;
						return aResponse;

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
		return null;
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
	public byte[] scrambleData(byte[] aByte) {
		aByte[0] ^= decodeSeed;
		for (int x = 1; x < aByte.length; x++) {
			aByte[x] ^= aByte[x - 1];
		}

		return aByte;
	}

	/**
	 * The new HNClient 4.1.d sends data in a scrambled form after the initial
	 * HandShaking data (12 bytes), and random 8 bytes (total 20 bytes) are sent
	 * This method unscrambles the data using exclusive OR and a decode seed The
	 * decode seed is derived from the random set of 8 bytes (it is also used in
	 * data scrambling)
	 *
	 * @param scrambleByte byte[]
	 * @return
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
		aMessage = DATA_INDICATOR + TEN_ZEROS.substring(0, TEN_ZEROS.length() - lengthOfMessage.length())
				+ lengthOfMessage + aMessage;
		return aMessage;
	}

	/**
	 * @return byte[]
	 * @throws java.net.UnknownHostException
	 */
	private String getHostName() throws UnknownHostException {
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
		return ipAddress.toString();
	}

}
