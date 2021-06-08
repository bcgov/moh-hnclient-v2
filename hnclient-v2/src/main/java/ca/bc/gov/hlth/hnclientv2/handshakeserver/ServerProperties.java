package ca.bc.gov.hlth.hnclientv2.handshakeserver;

/**
 * Simple POJO to store server properties in one place.
 */
public class ServerProperties {

	private Integer serverSocket;
	private Integer socketReadSleepTime;
	private Integer maxSocketReadTries;
	private Integer threadPoolSize;

	public ServerProperties(Integer serverSocket, Integer socketReadSleepTime, Integer maxSocketReadTries, Integer threadPoolSize) {
		this.serverSocket = serverSocket;
		this.socketReadSleepTime = socketReadSleepTime;
		this.maxSocketReadTries = maxSocketReadTries;
		this.threadPoolSize = threadPoolSize;
	}

	public Integer getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(Integer serverSocket) {
		this.serverSocket = serverSocket;
	}

	public Integer getSocketReadSleepTime() {
		return socketReadSleepTime;
	}

	public void setSocketReadSleepTime(Integer socketReadSleepTime) {
		this.socketReadSleepTime = socketReadSleepTime;
	}

	public Integer getMaxSocketReadTries() {
		return maxSocketReadTries;
	}

	public void setMaxSocketReadTries(Integer maxSocketReadTries) {
		this.maxSocketReadTries = maxSocketReadTries;
	}

	public Integer getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(Integer threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

}
