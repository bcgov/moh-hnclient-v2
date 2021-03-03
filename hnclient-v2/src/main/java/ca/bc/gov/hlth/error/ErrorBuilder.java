package ca.bc.gov.hlth.error;

import java.sql.Timestamp;

import io.netty.util.internal.StringUtil;

public class ErrorBuilder {

	private static String HL7Error_Default = "MSH|^~\\\\&|UNKNOWNAPP|UNKNOWNCLIENT|HL7ERRORGEN|UNKNOWNFACILITY";
	
	private static String ack= "ACK";
	
	private static String version = "2.4\n";

	private static String msa = "MSA|AR|";

	private static String seperator3 = "|||";

	private static Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	private static String processingDomain = "D";

	private static String sendingFacility;

	private static String recievingFacility;

	public static String generateError(Integer errorCode, String v2Msg) {

		String errCode = ErrorCodes.retrieveEnumByValue(errorCode);
		return buildHTTPErrorMessage(errCode, v2Msg);

	}

	public static String buildHTTPErrorMessage(String errMsg,String v2Msg) {
		return buildMSH(v2Msg) +buildMSA(errMsg);
	}

	public static String buildDefaultErrorMessage(String errMsg) {
		return buildMSH(HL7Error_Default) +buildMSA(errMsg) ;
	}
	
	
	private static String buildMSA(String errMsg) {
		return msa+timestamp+"|"+errMsg+seperator3;
	}

	

	public static String buildMSH(String msh) {
		String parsedMessage;
		if (!StringUtil.isNullOrEmpty(msh)) {
			parsedMessage = msh;
		} else
			parsedMessage = HL7Error_Default;
		String[] arr = parsedMessage.split("\\|");
		String sendingApp = "HNCLIENT";
		if (arr[3] != null) {
			sendingFacility = arr[3];
		} else {
			sendingFacility = "UNKNOWN";
		}
		String recievingApp = "HNCLIENT";
		if (arr[5] != null) {
			recievingFacility = arr[5];
		} else {
			recievingFacility = "UNKNOWN";
		}

		String msh1 = arr[0] + "|" + arr[1] + "|" + recievingApp + "|" + recievingFacility + "|" + sendingApp + "|"
				+sendingFacility+"|"+ack+seperator3+processingDomain+"|"+version;
		System.out.println(msh1);
		return msh1;

	}

}
