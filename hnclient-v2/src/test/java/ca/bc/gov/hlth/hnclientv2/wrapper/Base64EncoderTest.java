package ca.bc.gov.hlth.hnclientv2.wrapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import ca.bc.gov.hlth.hnclientv2.error.NoInputHL7Exception;

public class Base64EncoderTest extends CamelTestSupport {

    private final String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
            + "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
            + "PID||1234567890^^^BC^PH";

    private final String encodedMsg = "MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==";


    // Define a route with the Base64Encoder
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:sampleInput")
                        .setBody().method(new Base64Encoder())
                        .to("mock:output");
            }
        };
    }

    @Test
    public void testEncodeV2Message() throws Exception {

        // Define the route endpoint
        MockEndpoint mock = getMockEndpoint("mock:output");

        // Expect a base64 encoded message at the endpoint
        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived(encodedMsg);

        // When we send in a valid v2 message
        template.sendBody("direct:sampleInput", v2Msg);
        assertMockEndpointsSatisfied();

    }

    @Test
    public void encodeBase64_nullMessageTest() {
        try {
            // When we send in a null v2 message to a route with the Base64Encoder
            template.sendBody("direct:sampleInput", null);
        } catch (CamelExecutionException e ){
            // We expect an illegal argument exception
            // Because we have no exception handling in our simple test route camel throws
            // the IllegalArgumentException upwards as a CamelExecutionException so we check the cause
            assertTrue(e.getCause() instanceof NoInputHL7Exception);
        }
    }

    @Test
    public void encodeBase64_blankMessageTest() {
        try {
            // When we send in a null v2 message to a route with the Base64Encoder
            template.sendBody("direct:sampleInput", "");
        } catch (CamelExecutionException e ){
            // We expect an illegal argument exception
            // Because we have no exception handling in our simple test route camel throws
            // the IllegalArgumentException upwards as a CamelExecutionException so we check the cause
            assertTrue(e.getCause() instanceof NoInputHL7Exception);
        }
    }

}
