package ca.bc.gov.hlth.hnclientv2.error;

import java.util.HashMap;
import java.util.Map;

public enum ErrorCodes {

	// TODO: Verify error codes with Patrick
	SERVER_ERROR(500, "Error connecting to SERVER"), HOST_UNAVAILABLE(401, "Could not connect with the remote host"),
	REMOTE_TIMEOUT_ERROR(402, "Connection with remote facility timed out"), UNAUTHORIZED(404, "Unauthorized");

	private int errorCode;
	private String errorMessage;
	public static Map<Integer, ErrorCodes> errorCodeByErrorNumber = new HashMap<Integer, ErrorCodes>();

	static {
		for (ErrorCodes errorCode : ErrorCodes.values()) {
			errorCodeByErrorNumber.put(errorCode.getErrorCode(), errorCode);
		}
	}

	private ErrorCodes(int errorCode, String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public static ErrorCodes getErrorCodeByNumber(Integer errorNumber) {
		return errorCodeByErrorNumber.get(errorNumber);
	}

	public static String retrieveEnumByValue(Integer errorCode) {
		if (errorCode == null)
			return "";
		ErrorCodes businessError = ErrorCodes.getErrorCodeByNumber(errorCode);
		return businessError.getErrorMessage();
	}

}
