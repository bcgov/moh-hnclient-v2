package ca.bc.gov.hlth.hnclientv2.error;

import java.sql.Timestamp;

import org.apache.commons.lang3.StringUtils;



public class ErrorBuilder {

	private static String HL7ERROR_DEFAULT = "MSH|^~\\\\&|UNKNOWNAPP|UNKNOWNCLIENT|HL7ERRORGEN|UNKNOWNFACILITY";
	
	private static String ACK = "ACK";
	
	private static String MSH = "MSH";

	private static String MSA = "MSA|AR|";
	
	private static String UNKNOWN = "UNKNOWN";
	
	private static String SENDING_RECEIVING_APP = "HNCLIENT";
	
	private  static String DOUBLE_BACKSLASH = "\\"; // For using specific string in regex mathces
	
	private  static String DELIMITER = "|";

	private static Timestamp TIMESTAMP = new Timestamp(System.currentTimeMillis());


	
	@Deprecated
	public static String generateError(Integer errorCode, String v2Msg) {

		String errCode = ErrorCodes.retrieveEnumByValue(errorCode);
		return buildErrorMessage(errCode, v2Msg);

	}

	/**
	 * @param v2Msg
	 * @param errMsg
	 * @return
	 */
	public static String buildErrorMessage(String v2Msg,String errMsg) {
		return buildMSH(v2Msg) +buildMSA(v2Msg,errMsg);
	}

	private static String buildMSA(String v2Msg, String errMsg) {
		if(StringUtils.isBlank(v2Msg)) {
			v2Msg= HL7ERROR_DEFAULT;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(MSA);
		sb.append(getMsgId(v2Msg.split(DOUBLE_BACKSLASH+DELIMITER)));
		sb.append(DELIMITER);
		sb.append(StringUtils.defaultString(errMsg));
		sb.append(DELIMITER);
		sb.append(DELIMITER);
		sb.append(DELIMITER);
		return sb.toString();
	}	

	/**
	 * @param messageHeader
	 * @return
	 */
	public static String buildMSH(String messageHeader) {
		String v2Message;
		
		if (!StringUtils.isBlank(messageHeader)) {
			String[] arr = messageHeader.split(DOUBLE_BACKSLASH+DELIMITER);
			if(arr.length>1 && arr[0].equalsIgnoreCase(MSH)) {
				v2Message = messageHeader;
			}
			else {
				v2Message = HL7ERROR_DEFAULT;
			}
		} else {
			v2Message = HL7ERROR_DEFAULT;
		}
		
		String[] dataSegments = v2Message.split(DOUBLE_BACKSLASH+DELIMITER);				
		String responseMSH = buildErrorResponse(dataSegments);	
		return responseMSH;

	}	

	/**
	 * @param parsedMessage
	 * @param dataSegments
	 * @return
	 */
	private static String buildErrorResponse(String[] dataSegments) {		
		StringBuilder sb = new StringBuilder();
		sb.append(MSH);
		sb.append(DELIMITER);
		sb.append(dataSegments[1]);
		sb.append(DELIMITER);
		sb.append(SENDING_RECEIVING_APP);
		sb.append(DELIMITER);
		sb.append(getReceivingFacility(dataSegments));
		sb.append(DELIMITER);
		sb.append(SENDING_RECEIVING_APP);
		sb.append(DELIMITER);
		sb.append(getSendingFacility(dataSegments));
		sb.append(DELIMITER);
		sb.append(TIMESTAMP);
		sb.append(DELIMITER);
		sb.append(getuserId(dataSegments));
		sb.append(DELIMITER);
		sb.append(ACK);
		sb.append(DELIMITER);
		sb.append(getMsgType(dataSegments));
		sb.append(DELIMITER);
		sb.append(getMsgId(dataSegments));
		sb.append(DELIMITER);
		sb.append(getProcessigId(dataSegments));
		sb.append(DELIMITER);
		sb.append(getVersion(dataSegments));
		
		sb.append("\n");
		return sb.toString();
		
	}
		
	/**
	 * returns the message id based on the HL7 message.
	 * @param hlMsg
	 * @return
	 */
	private static String getMsgId(String[] dataSegments) {
		String msgId = "";		
		if (dataSegments.length > 9) {
			msgId = dataSegments[9];
		}	
		return msgId;
	}
	
	/**
	 * returns the message id based on the HL7 message.
	 * @param hlMsg
	 * @return
	 */
	private static String getProcessigId(String[] dataSegments) {
		String processingId = "";		
		if (dataSegments.length > 10) {
			processingId = dataSegments[10];
		}	
		return processingId;
	}
	
	/**
	 * @param dataSegments
	 * @return
	 */
	private static String getuserId(String[] dataSegments) {
		String usrId = ""; 
		if(dataSegments.length>6 ) {
			usrId = dataSegments[7];	
		}
		return usrId;
	}

	/**
	 * @param dataSegments
	 */
	private static String getReceivingFacility(String[] dataSegments) {
		String receivingFacility;
		if (dataSegments.length>5) {
			receivingFacility = dataSegments[5];
		} else {
			receivingFacility = UNKNOWN;
		}
		return receivingFacility;
	}

	/**
	 * @param dataSegments
	 */
	private static String getSendingFacility(String[] dataSegments) {
		String sendingFacility;
		if (dataSegments.length>3) {
			sendingFacility = dataSegments[3];
		} else {
			sendingFacility = UNKNOWN;
		}
		return sendingFacility;
	}
	
	/**
	 * @param dataSegments
	 */
	private static String getVersion(String[] dataSegments) {
		String version;
		if (dataSegments.length>11) {
			version = dataSegments[11];
		} else {
			version = "";
		}
		return version;
	}
	
	/**
	 * @param dataSegments
	 */
	private static String getMsgType(String[] dataSegments) {
		String msgType;
		if (dataSegments.length>8) {
			msgType = dataSegments[8];
		} else {
			msgType = "";
		}
		return msgType;
	}

}



