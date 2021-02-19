package ca.bc.gov.hlth.hnclientv2.route;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import ca.bc.gov.hlth.error.ErrorProcessor;
import ca.bc.gov.hlth.hnclientv2.wrapper.Base64Encoder;
import ca.bc.gov.hlth.hnclientv2.wrapper.ProcessV2ToJson;

public class ErrorBuilderRouteTest extends CamelTestSupport {

	private String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
			+ "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n" + "PID||1234567890^^^BC^PH";

	@Override
	protected RouteBuilder createRouteBuilder() {

		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				from("direct:sampleInput").setBody().method(new Base64Encoder()).process(new ProcessV2ToJson())
						.id("ProcessV2ToJson").log("v2Message encoded to Base64 format").log("Sending to HNSecure")
						.process(new ErrorProcessor()).to("mock:output");
			}
		};
	}
	

	private void extractErrorMessage(String expectedErrorMsg, Integer header) throws Exception {
		MockEndpoint mock = getMockEndpoint("mock:output");
		mock.expectedMessageCount(1);
		mock.expectedMessagesMatches(new Predicate() {

			@Override
			public boolean matches(Exchange exchange) {
				String obj = (String) exchange.getIn().getBody();
				String[] arr = obj.split("\\|");

				return (arr[12].equals(expectedErrorMsg));
			}
		});

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("CamelHttpResponseCode", header);
		template.sendBodyAndHeaders("direct:sampleInput", v2Msg, headers);// triggering
																			// route execution by sending input to route
		assertMockEndpointsSatisfied(); // Verifies if input is equal to output
	}

	@Test
	public void testErrorBuilder_header401() throws Exception {

		String expectedErrorMsg = "^^^Unidentified Type\\Error:Could not connect with the remote host";
		Integer header = 401;
		extractErrorMessage(expectedErrorMsg, header);

	}


	@Test
	public void testErrorBuilder_header500() throws Exception {

		String expectedErrorMsg = "^^^Unidentified Type\\Error:Error connecting to SERVER";
		Integer header = 500;
		extractErrorMessage(expectedErrorMsg, header);

	}

	@Test
	public void testErrorBuilder_header402() throws Exception {

		String expectedErrorMsg = "^^^Unidentified Type\\Error:Connection with remote facility timed out";
		Integer header = 402;
		extractErrorMessage(expectedErrorMsg, header);

	}

	@Test
	public void testErrorBuilder_headerNull() throws Exception {

		String expectedErrorMsg = "^^^Unidentified Type\\Error:UNKNOWN";
		Integer header = null;
		extractErrorMessage(expectedErrorMsg, header);

	}

	@Test
	public void testErrorBuilder_header200() throws Exception {

		String expectedErrorMsg = "{\"content\":[{\"attachment\":{\"data\":\"MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";
		MockEndpoint mock = getMockEndpoint("mock:output");
		mock.expectedMessageCount(1);
		mock.expectedMessagesMatches(new Predicate() {

			@Override
			public boolean matches(Exchange exchange) {
				String obj = (String) exchange.getIn().getBody();
				String[] arr = obj.split("\\|");

				return (arr[0].equals(expectedErrorMsg));
			}
		});

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("CamelHttpResponseCode", 200);
		template.sendBodyAndHeaders("direct:sampleInput", v2Msg, headers);// triggering
																			// route execution by sending input to route

		assertMockEndpointsSatisfied(); // Verifies if input is equal to output

	}

}
