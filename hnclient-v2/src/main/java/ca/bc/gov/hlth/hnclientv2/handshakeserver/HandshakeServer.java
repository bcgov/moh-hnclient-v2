package ca.bc.gov.hlth.hnclientv2.handshakeserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hncommon.util.LoggingUtil;

/**
 * This class connects to listener, does handshake and sends request to ESB.
 * Receives the response from ESB and send it to originator.
 * @author anumeha.srivastava
 *
 */
public class HandshakeServer {

	private static final Logger logger = LoggerFactory.getLogger(HandshakeServer.class);
	
	private ProducerTemplate producer;
	
	private ServerProperties properties;

	public HandshakeServer(ProducerTemplate producer, ServerProperties properties) {
		this.producer = producer;
		this.properties = properties;
		initConnectionHandler();
	}

	/**
	 * This function accepts connection from client , calls handshake method
	 * and if handshake is successful, performs HL7 transaction
	 * if Handshake fails then it sends a error response to client
	 */
	private void initConnectionHandler() {

		// Run the ServerSocket in it's own Thread so that it's not blocking the rest of Camel
		Runnable serverTask = new Runnable() {

			@Override
			public void run() {
				ThreadPoolExecutor executor = new ThreadPoolExecutor(properties.getThreadPoolSize(), properties.getThreadPoolSize(),
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());
				
				ServerSocket mysocket = null;
				try {					
					mysocket = new ServerSocket(properties.getServerSocket());
					while (true) {
						Socket connectionSocket = mysocket.accept();
						logger.info("{} - Accepting connection attempt from IP Address: {}", LoggingUtil.getMethodName(),
								connectionSocket.getInetAddress().getCanonicalHostName());
						
						ConnectionHandler handler = new ConnectionHandler(producer, connectionSocket, properties.getSocketReadSleepTime(), properties.getMaxSocketReadTries());
						
						Integer activeCount = executor.getActiveCount();
						
						logger.debug("{} threads are active", activeCount);
						if (activeCount == properties.getThreadPoolSize()) {
							logger.info("Max Thread Pool Size reached. Waiting for available thread");
						}
						// Checking the queue after submission might seem to give a more accurate count
						// but at that point the submitted item is always in the queue (albeit briefly)
						Integer queueSize = executor.getQueue().size();
						if (queueSize > 0) {
							logger.debug("{} items waiting in queue", queueSize);
						};
						executor.submit(handler);
						
						logger.info("{} - Started thread to handle HL7Xfer connection on socket: {}", LoggingUtil.getMethodName(),
								properties.getServerSocket());

					}
				} catch (IOException ioe) {
					logger.error("{} - Could not start ServerSocket on {}. Error: {}", LoggingUtil.getMethodName(), properties.getServerSocket(), ioe.getMessage());
					throw new RuntimeCamelException("Could not start HandshakeServer", ioe);
				} finally {
					if (mysocket != null) {
						try {
							mysocket.close();
						} catch (IOException e) {
							logger.warn("ServerSocket could not be closed");
						}
					}
					executor.shutdown();
				}

			}

		};
		Thread serverThread = new Thread(serverTask);

		Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
			
		    @Override
		    public void uncaughtException(Thread th, Throwable ex) {
		    	// XXX We don't want to end up here as this means the error was not caught
		    	// and we have no good way to recover from it.
		    	logger.error("Unhandled exception {} in thread {}", ex.getMessage(), th.getId());
		    }
		};
		
		serverThread.setUncaughtExceptionHandler(handler);
		
		serverThread.start();
	}

}
