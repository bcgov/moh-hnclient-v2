package ca.bc.gov.hlth.hnclientv2.error;

public class NoInputHL7Exception extends CamelCustomException {

	private static final long serialVersionUID = 1L;

	public NoInputHL7Exception() {
		super();
	}

	public NoInputHL7Exception(String msg) {
		super(msg);
	}
}
