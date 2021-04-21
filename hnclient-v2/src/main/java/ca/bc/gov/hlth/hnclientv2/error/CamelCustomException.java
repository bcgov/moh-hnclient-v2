package ca.bc.gov.hlth.hnclientv2.error;

public class CamelCustomException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public CamelCustomException(String msg) {
		super(msg);
	}
    
}

