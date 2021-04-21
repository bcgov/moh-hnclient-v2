package ca.bc.gov.hlth.hnclientv2.wrapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

public class ProcessV2ToJsonTest extends CamelTestSupport {
	
	String encodeV2Msg = "MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==";

	// Define a route with the ProcessV2ToJson
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:sampleInput")
				.process(new ProcessV2ToJson())
                .to("mock:output");
			}
		};
	}

	@Test
	public void testV2MsgFlow() throws Exception {
		String expectedMsg = "{\"content\":[{\"attachment\":{\"data\":\"MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";
		// Define the route endpoint
		MockEndpoint mock = getMockEndpoint("mock:output");

		// Expect the json encoded message at the endpoint
		mock.expectedMessageCount(1);
		mock.expectedBodiesReceived(expectedMsg);

		// When we send in a Base64Encoded v2 message
	    template.sendBody("direct:sampleInput",encodeV2Msg );	
	    assertMockEndpointsSatisfied();
	}

	@Test
	public void processV2ToJson_nullMessageTest() {
		try {
			// When we send in a null v2 message to a route with the ProcessV2ToJson processor
			template.sendBody("direct:sampleInput", null);
		} catch (CamelExecutionException e ){
			// We expect a null pointer exception
			// Because we have no exception handling in our simple test route camel throws
			// the IllegalArgumentException upwards as a CamelExecutionException so we check the cause
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}
	
	
}
