package ca.bc.gov.hlth.hnclientv2.error;

public class ServerNoConnectionException extends CamelCustomException {

    public ServerNoConnectionException(String msg) {
        super(msg);
    }
}
