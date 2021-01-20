
import static org.junit.Assert.assertTrue;

import java.util.Base64;

import org.junit.Test;

import ca.bc.gov.hlth.hnclientv2.Base64Encoder;

public class Base64Test {
	
	private String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
            + "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
            + "PID||1234567890^^^BC^PH";
	
	@Test
	public void test_encode() throws Exception {
		
		String convertToBase64String = Base64Encoder.convertToBase64String(v2Msg);
		String decode = new String(ConvertFromBase64String(convertToBase64String));
		assertTrue(v2Msg.equals(decode));
		
	}
	
	public  byte[] ConvertFromBase64String(String base64EncodedString) throws Exception
    {
        if (base64EncodedString == null || base64EncodedString.length() == 0)
            throw new IllegalArgumentException("You must supply byte string for Base64 decoding operation");

        if (base64EncodedString.length() % 4 != 0)
            throw new IllegalArgumentException("The BASE-64 encoded data is not in correct form (divide by 4 resulted in a remainder)");

        try {
            return Base64.getDecoder().decode(base64EncodedString);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to decode Base-64 string supplied for operation. Please check your inputs");
        }
    }


}
