package ca.bc.gov.hlth.hnclientv2.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

public class FhirPayloadExtractorTest extends CamelTestSupport {

	private static final String fhirJsonMsg = "{\"content\":[{\"attachment\":{\"data\":\"MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";
	
    private static final String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
            + "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
            + "PID||1234567890^^^BC^PH";
	
	// Define a route with the FhirPayloadExtractor
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:sampleInput")
				.setBody().method(new FhirPayloadExtractor())
                .to("mock:output");
			}
		};
	}

	@Test
	public void testV2MsgFlow() throws Exception {
		// Define the route endpoint
		MockEndpoint mock = getMockEndpoint("mock:output");

		// Expect the json encoded message at the endpoint
		mock.expectedMessageCount(1);
		mock.expectedBodiesReceived(v2Msg);

		// When we send in a Base64Encoded v2 message
	    template.sendBody("direct:sampleInput", fhirJsonMsg);	
	    assertMockEndpointsSatisfied();
	}

	@Test
	public void processV2ToJson_nullMessageTest() {
		try {
			// When we send in a null v2 message to a route with the FhiPayloadExtractor processor
			template.sendBody("direct:sampleInput", null);
		} catch (CamelExecutionException e ){
			// We expect a null pointer exception
			// Because we have no exception handling in our simple test route camel throws
			// the NullPointerException upwards as a CamelExecutionException so we check the cause
			assertTrue(e.getCause() instanceof NullPointerException);
		}
	}
	
	
}
