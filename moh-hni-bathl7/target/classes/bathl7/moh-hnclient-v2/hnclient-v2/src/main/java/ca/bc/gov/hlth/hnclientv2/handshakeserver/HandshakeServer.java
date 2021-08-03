package ca.bc.gov.hlth.hnclientv2.handshakeserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hnclientv2.TransactionIdGenerator;
import ca.bc.gov.hlth.hncommon.util.LoggingUtil;

/**
 * This class connects to listener, does handshake and sends request to ESB.
 * Receives the response from ESB and send it to originator.
 * 
 * @author anumeha.srivastava
 *
 */
public class HandshakeServer {

	private static final Logger logger = LoggerFactory.getLogger(HandshakeServer.class);

	private ProducerTemplate producer;

	private ServerProperties properties;

	private String localIpAddress;

	private String loopbackIpAddress;
	
	private String transactionId;

	private List<String> validIpList = new ArrayList<String>();

	public HandshakeServer(ProducerTemplate producer, ServerProperties properties) {
		this.producer = producer;
		this.properties = properties;
		initAccessControl();
		initConnectionHandler();
	}
	
	private void initAccessControl() {
		String methodName = LoggingUtil.getMethodName();
		try {
			localIpAddress = InetAddress.getLocalHost().getHostAddress();			
			logger.debug("{} - Local IP Address {}", methodName,localIpAddress);

			loopbackIpAddress = InetAddress.getLoopbackAddress().getHostAddress();			
			logger.debug("{} - Loopback IP Address {}", methodName,loopbackIpAddress);
		} catch (UnknownHostException e) {
			logger.error("{} - Could not get host address", methodName );
		}
		
		if (StringUtils.isBlank(properties.getValidIpListFile())) {
			// This is a valid condition
			logger.info("{} - No validIpListFile was configured", methodName);
			return;
		}

		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(properties.getValidIpListFile())) {
			// There's a simpler way to do this using Path and Files but it doesn't work well in an uber jar
			// without extraneous additional code
			validIpList = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
					.collect(Collectors.toList());
			logger.info("{} - Loaded {} valid IP addresses from {}", methodName, validIpList.size(), properties.getValidIpListFile());
		} catch (Exception e) {
			logger.error("{} - Could not load validIpList. Error: {}", methodName, e.getMessage());
			throw new RuntimeCamelException("Could not load Valid IP List from " + properties.getValidIpListFile() + ". Please check configuration.");
		}
	}

	/**
	 * This function accepts connection from client , calls handshake method and if
	 * handshake is successful, performs HL7 transaction if Handshake fails then it
	 * sends a error response to client
	 */
	private void initConnectionHandler() {

		// Run the ServerSocket in it's own Thread so that it's not blocking the rest of
		// Camel
		Runnable serverTask = new Runnable() {

			@Override
			public void run() {
				String methodName = LoggingUtil.getMethodName();
				ThreadPoolExecutor executor = new ThreadPoolExecutor(properties.getThreadPoolSize(), properties.getThreadPoolSize(), 0L,
						TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

				ServerSocket mysocket = null;
				try {
					mysocket = new ServerSocket(properties.getServerSocket());
					while (true) {
						Socket connectionSocket = mysocket.accept();

						String hostAddress = connectionSocket.getInetAddress().getHostAddress();
						
						transactionId = new TransactionIdGenerator().generateUuid();

						logger.info("{} - TransactionId: {}, Accepting connection attempt from IP Address: {}", methodName, transactionId,
								hostAddress);

						// Local connections are always allowed so only validate remote connections
						if (!isLocalConnection(hostAddress)) {
							if (!properties.getAcceptRemoteConnections()) {
								logger.warn("{} - TransactionId: {}, Remote connection rejected: Remote connections not allowed", methodName, transactionId);
								continue;
							} else {
								if (!isValidIp(hostAddress)) {
									logger.warn("{} - TransactionId: {}, Remote connection rejected. IP address {} not in valid list", methodName, transactionId,  hostAddress);
									continue;	
								}
							}
						}

						ConnectionHandler handler = new ConnectionHandler(producer, connectionSocket, properties.getSocketReadSleepTime(),
								properties.getMaxSocketReadTries(), transactionId);

						Integer activeCount = executor.getActiveCount();

						logger.debug("{} - TransactionId: {} threads are active",methodName, transactionId,  activeCount);
						if (Objects.equals(activeCount, properties.getThreadPoolSize())) {
							logger.info("{} - TransactionId: {} Max Thread Pool Size reached. Waiting for available thread", methodName, transactionId);
						}
						// Checking the queue after submission might seem to give a more accurate count
						// but at that point the submitted item is always in the queue (albeit briefly)
						Integer queueSize = executor.getQueue().size();
						if (queueSize > 0) {
							logger.debug("{} - TransactionId: {} {} items waiting in queue", methodName, transactionId, queueSize);
						}

						executor.submit(handler);

						logger.info("{} - Started thread to handle HL7Xfer connection on socket: {}", methodName,
								properties.getServerSocket());

					}
				} catch (IOException ioe) {
					logger.error("{} - TransactionId: {}  Could not start ServerSocket on {}. Error: {}", methodName, transactionId,
							properties.getServerSocket(), ioe.getMessage());
					throw new RuntimeCamelException("Could not start HandshakeServer", ioe);
				} finally {
					if (mysocket != null) {
						try {
							mysocket.close();
						} catch (IOException e) {
							logger.warn("{} - TransactionId: {} ServerSocket could not be closed", methodName, transactionId);
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
				logger.error("{} - TransactionId: {} Unhandled exception {} in thread {}", LoggingUtil.getMethodName(), transactionId, ex.getMessage(), th.getId());
			}
		};

		serverThread.setUncaughtExceptionHandler(handler);

		serverThread.start();
	}

	private Boolean isLocalConnection(String hostAddress) {
		return StringUtils.equals(hostAddress, loopbackIpAddress)
				|| StringUtils.equals(hostAddress, localIpAddress);
	}

	private Boolean isValidIp(String hostAddress) {
		return validIpList.contains(hostAddress);
	}

}
