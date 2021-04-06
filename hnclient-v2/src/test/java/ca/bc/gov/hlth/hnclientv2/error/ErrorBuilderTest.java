package ca.bc.gov.hlth.hnclientv2.error;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import ca.bc.gov.hlth.hnclientv2.wrapper.Base64Encoder;
import ca.bc.gov.hlth.hnclientv2.wrapper.ProcessV2ToJson;

public class ErrorBuilderTest extends CamelTestSupport {

	private final String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
			+ "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n" + "PID||1234567890^^^BC^PH";

	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:sampleInput")
						.setBody().method(new Base64Encoder())
						.process(new ProcessV2ToJson()).id("ProcessV2ToJson")
						.process(new ErrorProcessor())
						.to("mock:output");
			}
		};
	}

	@Test
	public void testErrorBuilder_header200() throws Exception {

		String expectedMsg = "{\"content\":[{\"attachment\":{\"data\":\"MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";

		// Define the route endpoint
		MockEndpoint mock = getMockEndpoint("mock:output");
		// Expect to receive no error message
		mock.expectedMessageCount(1);
		mock.expectedBodiesReceived(expectedMsg);

		Map<String, Object> headers = new HashMap<>();
		headers.put("CamelHttpResponseCode", 200);
		// When we send a message to the Error Processor with a 200 Response Header
		template.sendBodyAndHeaders("direct:sampleInput", v2Msg, headers);// triggering

		// Validate the assertions
		assertMockEndpointsSatisfied();
	}
	

	private void extractErrorMessage(String expectedErrorMsg, Integer header) throws Exception {

		// Define the route endpoint
		MockEndpoint mock = getMockEndpoint("mock:output");
		// Expect an error message that matches our expectedErrorMsg in the 14th segment of the v2 message
		mock.expectedMessageCount(1);
		mock.expectedMessagesMatches(exchange -> {
			String obj = (String) exchange.getIn().getBody();
			String[] arr = obj.split("\\|");

			return (arr[14].equals(expectedErrorMsg));
		});

		Map<String, Object> headers = new HashMap<>();
		headers.put("CamelHttpResponseCode", header);
		// When we send in a message with an error code in the response headers (e.g. 401)
		template.sendBodyAndHeaders("direct:sampleInput", v2Msg, headers);

		// Validate the assertions
		assertMockEndpointsSatisfied();
	}

	@Test
	public void testErrorBuilder_header401() throws Exception {
		String expectedErrorMsg = "Unauthorized";
		Integer responseCodeHeader = 401;
		extractErrorMessage(expectedErrorMsg, responseCodeHeader);
	}

	@Test
	public void testErrorBuilder_header500() throws Exception {
		String expectedErrorMsg = "Internal Server Error";
		Integer responseCodeHeader = 500;
		extractErrorMessage(expectedErrorMsg, responseCodeHeader);
	}

	@Test
	public void testErrorBuilder_headerNull() throws Exception {
		String expectedErrorMsg = "An unknown error has occurred.";
		Integer responseCodeHeader = null;
		extractErrorMessage(expectedErrorMsg, responseCodeHeader);
	}
}
