package ca.bc.gov.hlth.error;

import java.sql.Timestamp;

public class ErrorBuilder {

	private static String HL7Error_Default = "MSH|^~\\\\&|UNKNOWNAPP|UNKNOWNCLIENT|HL7ERRORGEN|UNKNOWNFACILITY|||ACK|||2.4\n";

	private static String section1 = "ERR|^^^Unidentified Type\\Error:";

	private static String section2 = "|UNKNOWN|UNKNOWN|";

	private static String section3 = "|||";

	private static Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	private static String processingDomain = "D";

	public static String generateError(Integer errorCode) {

		String err = ErrorCodes.retrieveEnumByValue(errorCode);
		return buildHTTPErrorMessage(err);

	}

	public static String buildHTTPErrorMessage(String msg) {
		return HL7Error_Default + section1 + msg + section2 + timestamp + section3 + processingDomain ;
	}
	
	public static String buildDefaultErrorMessage(String msg) {
		return HL7Error_Default + section1 + msg + section2 + timestamp + section3 + processingDomain;
	}

}
