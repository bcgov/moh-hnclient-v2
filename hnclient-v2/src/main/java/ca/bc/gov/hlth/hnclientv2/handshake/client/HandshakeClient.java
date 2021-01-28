package ca.bc.gov.hlth.hnclientv2.handshake.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HandshakeClient {

	private static Socket netSocket = null;

	private static byte decodeSeed = 0;

	// HandShake Segment
	private static byte handShakeSegment[] = new byte[12];
	private static byte handShakeData[] = new byte[8];

	public static void main(String args[])
      {
	   final int SOCKET_READ_SLEEP_TIME = 100; // milliseconds
	    final int MAX_SOCKET_READ_TRIES = 100; // total of 10 seconds
	    int numSocketReadTries = 0;
	   
	   try{
		   
	    System.out.println("Client: "+"Connection Established");
	    Socket netSocket = new Socket("127.0.0.1", 5555);
	    
	    //netSocket = new Socket("127.0.0.1", 55286);
	     BufferedInputStream socketInput =
	          new BufferedInputStream(netSocket.getInputStream(), 1000);
	      BufferedOutputStream socketOutput =
	          new BufferedOutputStream(netSocket.getOutputStream());      
	      
	      System.out.println("Waitng for HNClient");
	   // Wait for data from HNClient
	      while (socketInput.available() < 1 && numSocketReadTries < MAX_SOCKET_READ_TRIES) {
	        numSocketReadTries++;
	        java.lang.Thread.sleep(SOCKET_READ_SLEEP_TIME);
	        System.out.println("Waiting for Listener ");
	      }

	      // data available for read in BufferedInputStream
	      if (socketInput.available() > 0) {    	  
	        // Read 12 bytes of HandShake Segment and 8 bytes of HandShake data
	    	System.out.println("Reading handshake data client side");
	        socketInput.read(handShakeSegment, 0, 12);	        
	        socketInput.read(handShakeData, 0, 8);
	      
	        // write 12 bytes of HS response to BufferedOutputStream
	        socketOutput.write("HS0000000008".getBytes(),0,12);
	        System.out.println("Wrote HSSegment to listener");
	        
	        
	     // write 8 bytes of scrambled HandShake data to BufferedOutputStream   
	        socketOutput.write(scrambleData(handShakeData),0,8);  
	     // set decodeSeed to last byte of scrambled handShakeData
	     			decodeSeed = handShakeData[7];

	       // System.out.println("Wrote HSData to listener");	
	        socketOutput.flush();
	        
	        if (netSocket != null)
			{
				socketInput.close();
				socketOutput.close();
				netSocket.close();
			}

	      }
			

   }catch(Exception e)
	   {e.printStackTrace();}
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
	protected static byte[] scrambleData(byte[] aByte) {	
		String s = new String(aByte, StandardCharsets.UTF_8);
		System.out.println(s);
		aByte[0] ^= decodeSeed;
		for (int x = 1; x < aByte.length; x++) {
			aByte[x] ^= aByte[x - 1];
		}
		
		System.out.println(aByte);
		return aByte;
	}
	
	
	/**
	* This method overrides the finalize method.
	* <p>
	* It is a clean up method closing the socket if it is still open and destroying the timer thread if it is still
	* running.
	*/
	protected void finalize() throws Throwable
	{
		if (netSocket != null)
		{
			try
			{
				netSocket.close();
				netSocket = null;
			}
			catch (java.io.IOException e)
			{
			}
		}
		super.finalize();
	}

	
	
	/**
	   * Insert the method's description here. Creation date: (2/6/01 4:38:50 PM) (JLee)
	   *
	   * <p>The new HNClient 4.1.d sends data in a scrambled form after the initial HandShaking data (12
	   * bytes), and random 8 bytes (total 20 bytes) are sent This method unscrambles the data using
	   * exclusive OR and a decode seed The decode seed is derived from the random set of 8 bytes (it is
	   * also used in data scrambling)
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
	}



