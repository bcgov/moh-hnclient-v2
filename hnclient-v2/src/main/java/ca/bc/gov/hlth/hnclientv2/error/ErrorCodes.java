package ca.bc.gov.hlth.hnclientv2.error;

import java.util.HashMap;
import java.util.Map;

public enum ErrorCodes {

	// TODO: Verify error codes with Patrick
	
	BAD_REQUEST(400,"Bad Request"),
	UNAUTHORIZED(401,"Unauthorized"),
	FORBIDDEN(403,"Forbidden"),
	NOT_FOUND(404,"Not Found"),
	METHOD_NOT_ALLOWED(405,"Method Not Allowed"),
	REQUEST_TIMEOUT(408,"Request Timeout"),
	INTERNAL_SERVER_ERROR(500,"Internal Server Error"),
	BAD_GATEWAY(502,"Bad Gateway"),
	SERVICE_UNAVAILABLE(503,"Service Unavailable"),
	GATEWAY_TIMEOUT(504,"Gateway Timeout");
	

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
