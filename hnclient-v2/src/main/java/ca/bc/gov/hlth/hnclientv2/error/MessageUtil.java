package ca.bc.gov.hlth.hnclientv2.error;

public class MessageUtil {

	public static final String HNET_RTRN_SUCCESS = "HNET_RTRN_SUCCESS";

	public static final String HNET_RTRN_INVALIDPARAMETER = "HNET_RTRN_INVALIDPARAMETER";

	public static final String HNET_RTRN_INVALIDFORMATERROR = "HNET_RTRN_INVALIDFORMATERROR";

	public static final String HNET_RTRN_REMOTETIMEOUT = "HNET_RTRN_REMOTETIMEOUT";

	public static final String HNET_RTRN_DOMAINMISMATCH = "HNET_RTRN_DOMAINMISMATCH";

	public static final String HNET_RTRN_ENCRYPTIONERROR = "HNET_RTRN_ENCRYPTIONERROR";

	public static final String HL7Error_Msg_MSHSegmentMissing = "The MSH Segment from the HL7 Message is missing.";

	public static final String HL7Error_Msg_ErrorDTHeaderToHNClient = "Unable to send the DT Header to HNCLIENT.";

	public static final String HL7Error_Msg_NoInputHL7 = "No HL7 Message was supplied as input";

	public static final String HL7Error_Msg_ServerUnavailable = "Protocol error: received an unexpected data segment from HL7Xfer Server.";

	public static final String SERVER_NO_CONNECTION = "Failed to connect remote server";

	public static final String UNKNOWN_EXCEPTION = "Unknown exception";

	public static final String INVALID_PARAMETER = "Wrong parameter";

}
