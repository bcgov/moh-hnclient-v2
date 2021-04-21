package ca.bc.gov.hlth.hnclientv2.error;

public class ServerNoConnectionException extends CamelCustomException {

	private static final long serialVersionUID = 1L;

	public ServerNoConnectionException(String msg) {
        super(msg);
    }
}
