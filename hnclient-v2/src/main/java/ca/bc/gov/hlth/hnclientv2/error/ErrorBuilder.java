package ca.bc.gov.hlth.hnclientv2.error;

import java.sql.Timestamp;

import org.apache.commons.lang3.StringUtils;

public class ErrorBuilder {

	private static String HL7Error_Default = "MSH|^~\\\\&|UNKNOWNAPP|UNKNOWNCLIENT|HL7ERRORGEN|UNKNOWNFACILITY";
	
	private static String ack= "ACK";
	
	private static String msh= "MSH";
	
	private static String version = "2.4";

	private static String msa = "MSA|AR|";
	
	private static String unknown = "UNKNOWN";
	
	private static String sendingOrReceivingApp = "HNCLIENT";
	
	private  static String double_backslash = "\\"; // For using specific string in regex mathces
	
	private  static String delimiter = "|";

	private static Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	private static String sendingFacility;

	private static String receivingFacility;
	
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
		StringBuilder sb = new StringBuilder();
		sb.append(msa);
		sb.append(getMsgId(v2Msg));
		sb.append(delimiter);
		sb.append(StringUtils.defaultString(errMsg));
		sb.append(delimiter);
		sb.append(delimiter);
		sb.append(delimiter);
		return sb.toString();
	}	

	/**
	 * @param messageHeader
	 * @return
	 */
	public static String buildMSH(String messageHeader) {
		String v2Message;
		
		if (!StringUtils.isBlank(messageHeader)) {
			String[] arr = messageHeader.split("\\|");
			if(arr.length>1 && arr[0].equalsIgnoreCase(msh)) {
				v2Message = messageHeader;
			}
			else
				v2Message = HL7Error_Default;
		} else
			v2Message = HL7Error_Default;
		
		String[] dataSegments = v2Message.split("\\|");				
		String responseMSH = buildErrorResponse(v2Message, dataSegments);	
		return responseMSH;

	}	

	/**
	 * @param parsedMessage
	 * @param dataSegments
	 * @return
	 */
	private static String buildErrorResponse(String parsedMessage, String[] dataSegments) {		
		StringBuilder sb = new StringBuilder();
		sb.append(msh);
		sb.append(getMsgId(dataSegments[0]));
		sb.append(delimiter);
		sb.append(dataSegments[1]);
		sb.append(delimiter);
		sb.append(sendingOrReceivingApp);
		sb.append(delimiter);
		sb.append(getReceivingFacility(dataSegments));
		sb.append(delimiter);
		sb.append(sendingOrReceivingApp);
		sb.append(delimiter);
		sb.append(getSendingFacility(dataSegments));
		sb.append(delimiter);
		sb.append(timestamp);
		sb.append(delimiter);
		sb.append(getuserId(dataSegments));
		sb.append(delimiter);
		sb.append(ack);
		sb.append(delimiter);
		sb.append(delimiter);
		sb.append(getProcessigId(parsedMessage));
		sb.append(delimiter);
		sb.append(version);
		
		sb.append("\n");
		return sb.toString();
		
	}
		
	/**
	 * returns the message id based on the HL7 message.
	 * @param hlMsg
	 * @return
	 */
	private static String getMsgId(String hlMsg) {

		String msgId = "";
		
		if (StringUtils.isEmpty(hlMsg)) {
			return msgId;
		}
		String[] hl7MessageAtt = hlMsg.split(double_backslash + delimiter);
		if (hl7MessageAtt.length > 9) {
			msgId = hl7MessageAtt[9];
		}
	
		return msgId;
	}
	
	/**
	 * returns the message id based on the HL7 message.
	 * @param hlMsg
	 * @return
	 */
	private static String getProcessigId(String hlMsg) {

		String msgId = "";
		
		if (StringUtils.isEmpty(hlMsg)) {
			return msgId;
		}
		String[] hl7MessageAtt = hlMsg.split(double_backslash+ delimiter);
		if (hl7MessageAtt.length > 10) {
			msgId = hl7MessageAtt[10];
		}
	
		return msgId;
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
		if (dataSegments.length>5) {
			receivingFacility = dataSegments[5];
		} else {
			receivingFacility = unknown;
		}
		return receivingFacility;
	}

	/**
	 * @param dataSegments
	 */
	private static String getSendingFacility(String[] dataSegments) {
		if (dataSegments.length>3) {
			sendingFacility = dataSegments[3];
		} else {
			sendingFacility = unknown;
		}
		return sendingFacility;
	}

}



