package ca.bc.gov.hlth.hnclientv2.error;

import java.sql.Timestamp;

import org.apache.commons.lang3.StringUtils;

import io.netty.util.internal.StringUtil;

public class ErrorBuilder {

	private static String HL7Error_Default = "MSH|^~\\\\&|UNKNOWNAPP|UNKNOWNCLIENT|HL7ERRORGEN|UNKNOWNFACILITY";
	
	private static String ack= "ACK";
	
	private static String msh= "MSH";

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
		if(StringUtil.isNullOrEmpty(v2Msg))
			v2Msg= HL7Error_Default;
		
		StringBuilder sb = new StringBuilder();
		sb.append(msa);
		sb.append(getMsgId(v2Msg.split(double_backslash+delimiter)));
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
			String[] arr = messageHeader.split(double_backslash+delimiter);
			if(arr.length>1 && arr[0].equalsIgnoreCase(msh)) {
				v2Message = messageHeader;
			}
			else
				v2Message = HL7Error_Default;
		} else
			v2Message = HL7Error_Default;
		
		String[] dataSegments = v2Message.split(double_backslash+delimiter);				
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
		sb.append(msh);
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
		sb.append(getMsgType(dataSegments));
		sb.append(delimiter);
		sb.append(getMsgId(dataSegments));
		sb.append(delimiter);
		sb.append(getProcessigId(dataSegments));
		sb.append(delimiter);
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
	
	/**
	 * @param dataSegments
	 */
	private static String getVersion(String[] dataSegments) {
		if (dataSegments.length>11) {
			sendingFacility = dataSegments[11];
		} else {
			sendingFacility = unknown;
		}
		return sendingFacility;
	}
	
	/**
	 * @param dataSegments
	 */
	private static String getMsgType(String[] dataSegments) {
		if (dataSegments.length>8) {
			sendingFacility = dataSegments[8];
		} else {
			sendingFacility = unknown;
		}
		return sendingFacility;
	}

}



