package ca.bc.gov.hlth.hnclientv2.unit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Base64;

import ca.bc.gov.hlth.hnclientv2.wrapper.Base64Encoder;

public class Base64EncoderTest {

    private String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
                            + "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
                            + "PID||1234567890^^^BC^PH";

    private String encodedMsg = "MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==";

    @Test
    public void test_encode() throws Exception {
        String convertToBase64String = new Base64Encoder().convertToBase64String(v2Msg);
        System.out.println(convertToBase64String);
        assertTrue(encodedMsg.equals(convertToBase64String));
    }

    /**
     * @param base64EncodedString
     * @return
     * @throws Exception
     */
    public byte[] ConvertFromBase64String(String base64EncodedString) throws Exception {
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
