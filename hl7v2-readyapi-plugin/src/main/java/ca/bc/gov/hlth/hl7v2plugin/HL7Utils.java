package ca.bc.gov.hlth.hl7v2plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for working with HL7v2 structure.
 */
public class HL7Utils {
	private static final Logger logger = LoggerFactory.getLogger(PluginConfig.LOGGER_NAME);

	private static final String LINE_BREAK = "\n";
	private static final String CARRIAGE_RETURN_LINE_BREAK = "\r\n";
	private final static String COMPONENT_DELIMITER = "^";

	public final static String FIELD_DELIMITER = "|";
	
	public static final String MESSAGE_TYPE_ZPN = "ZPN";
	
	public static final String SEGMENT_MSA = "MSA";
    public static final String SEGMENT_MSH = "MSH";
    public static final String SEGMENT_PID = "PID";
    public static final String SEGMENT_ZCB = "ZCB";
    public static final String SEGMENT_ZCC = "ZCC";

	public static final String[] MSA_ELEMENT_NAMES = new String[] {"Acknowledgement Code", "Message Control ID", "Text Message", "Expected Sequence Number",
			"Delayed Acknowledgement Type", "Error Condition", "Message Waiting Number", "Message Waiting Priority"};
    
	public static final String[] MSH_ELEMENT_NAMES = new String[] {"Field Separator", "Encoding Characters", "Sending Application", "Sending Facility",
			"Receiving Application", "Receiving Facility", "Date/Time Of Message", "Security", "Message Type", "Message Control ID", "Processing ID",
			"Version ID", "Sequence Number"};
	
	public static Map<String, String[]> parseToIndexedMap(String msg) {
		Map<String, String[]> segmentMap = new HashMap<String, String[]>();
		List<String> segments = getMessageSegments(msg);
		segments.forEach(segment -> {
			String[] fields = StringUtils.splitPreserveAllTokens(segment, FIELD_DELIMITER);
			// The first part of the segment is the identifier
			String segmentIdentifier = fields[0];

			if (StringUtils.equals(segmentIdentifier, SEGMENT_MSH)) {
				fields = adjustMSHSegment(fields);
			}
			logger.info(ArrayUtils.toString(fields));

			// XXX This will only support a single entry for each named segment
			segmentMap.put(segmentIdentifier, fields);
		});
		
		return segmentMap;
	}
	
	public static Map<String, Map<String, String>> parseToNamedMap(String msg) {
		Map<String, Map<String, String>> segmentMap = new HashMap<String, Map<String, String>>();
		List<String> segments = getMessageSegments(msg);

		// MSH
		String mshSegment = getSegment(segments, SEGMENT_MSH);
		String[] mshFields = StringUtils.splitPreserveAllTokens(mshSegment, FIELD_DELIMITER);
		
		Map<String, String> mshMap = new HashMap<String, String>();
		for (int i = 0; i < mshFields.length; i++) {
			mshMap.put(msg, MSH_ELEMENT_NAMES[i]);
		}
		segmentMap.put(SEGMENT_MSH, mshMap);

		// MSA
		String msaSegment = getSegment(segments, SEGMENT_MSA);
		String[] msaFields = StringUtils.splitPreserveAllTokens(msaSegment, FIELD_DELIMITER);
		
		Map<String, String> msaMap = new HashMap<String, String>();
		for (int i = 0; i < msaFields.length; i++) {
			msaMap.put(msg, MSA_ELEMENT_NAMES[i]);
		}
		segmentMap.put(SEGMENT_MSA, msaMap);
		
		return segmentMap;		
	}

	public static String[] parseToComponents(String field) {
		String[] components = StringUtils.splitPreserveAllTokens(field, HL7Utils.COMPONENT_DELIMITER);
		// Shift the components so they are 1 based, not 0 based
		return ArrayUtils.addAll(new String[] {""}, components);
	}

	private static String[] adjustMSHSegment(String fields[]) {
		// This code is a little odd since MSH-1 is actually the Field Separator itself. Since it was used as a split
		// delimiter we need to add it back explicitly
		String[] adjustedFields = ArrayUtils.insert(1, fields, FIELD_DELIMITER);
		return adjustedFields;
	}

	public static List<String> getMessageSegments(String v2Message) {
		String[] segments = null;
		if (v2Message.contains(CARRIAGE_RETURN_LINE_BREAK)) {
			segments = v2Message.split(CARRIAGE_RETURN_LINE_BREAK);
		} else if (v2Message.contains(LINE_BREAK)) {
			segments = v2Message.split(LINE_BREAK);
		} else {
			logger.warn("Can't split v2 message due to unknown EOL");
		}
		return Arrays.asList(segments);
	}
	
	public static String getSegment(List<String> segments, String segmentName) {
		for (String segment : segments) {						
			if (segment.startsWith(segmentName)) {
				return segment;
			}
		}
		logger.warn("Segment {} not found", segmentName);
		return null;
	}

	public static void main(String[] args) {
		String[] arr1 = {"MSH", "$", "data"};
		String[] arr2 = ArrayUtils.insert(1, arr1, "1");
		System.out.println(ArrayUtils.toString(arr2));
	}

}
