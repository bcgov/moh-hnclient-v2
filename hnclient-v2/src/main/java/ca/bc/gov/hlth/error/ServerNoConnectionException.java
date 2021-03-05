package ca.bc.gov.hlth.error;

public class ServerNoConnectionException extends CamelCustomException {

    public ServerNoConnectionException(String msg) {
        super(msg);
    }
}
