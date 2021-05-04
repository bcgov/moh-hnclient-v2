package ca.bc.gov.hlth.hnclientv2.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.jupiter.api.Test;

import ca.bc.gov.hlth.hnclientv2.wrapper.Base64Encoder;
import ca.bc.gov.hlth.hnclientv2.wrapper.ProcessV2ToJson;

public class FailureProcessorTest extends CamelTestSupport {

	private String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
			+ "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n" + "PID||1234567890^^^BC^PH";

	@Override
	protected RouteBuilder createRouteBuilder() {

		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {
		        onException(HttpHostConnectException.class).process(new FailureProcessor())
		        .log("Recieved body ${body}").handled(true);

		        onException(HttpOperationFailedException.class).process(new FailureProcessor())
		        .log("Recieved body ${body}").handled(true);
		        
		        onException(IllegalArgumentException.class).process(new FailureProcessor())
		        .log("Recieved body ${body}").handled(true);

		        onException(CamelCustomException.class).process(new FailureProcessor())
		        .log("Recieved body ${body}").handled(true);
		        
				from("direct:sampleInput")
				.setBody().method(new Base64Encoder())
				.setBody().method(new ProcessV2ToJson()).id("ProcessV2ToJson")
				.log("v2Message encoded to Base64 format")
				.log("Sending to HNSecure")
				.to("mock:output");
			}
		};
	}
	
	@Test
	public void testProcess_success_200() throws Exception {

		String expectedV2JSON = "{\"content\":[{\"attachment\":{\"data\":\"MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";
		MockEndpoint mock = getMockEndpoint("mock:output");
		mock.expectedMessageCount(1);
		mock.expectedMessagesMatches(new Predicate() {

			@Override
			public boolean matches(Exchange exchange) {
				String obj = (String) exchange.getIn().getBody();
				String[] arr = obj.split("\\|");

				return (arr[0].equals(expectedV2JSON));
			}
		});

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("CamelHttpResponseCode", 200);
		template.sendBodyAndHeaders("direct:sampleInput", v2Msg, headers);// triggering
																			// route execution by sending input to route

		assertMockEndpointsSatisfied(); // Verifies if input is equal to output

	}
	
	@Test
	public void testProcess_CamelCustomException() throws Exception {
		assertErrorMessage(v2Msg, MessageUtil.UNKNOWN_EXCEPTION, new Processor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				throw new CamelCustomException();
			}
			
		});
	}

	@Test
	public void testProcess_HttpHostConnectException() throws Exception {
		assertErrorMessage(v2Msg, MessageUtil.SERVER_NO_CONNECTION, new Processor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				throw new HttpHostConnectException(new IOException(), null);
			}
			
		});
	}
	
	@Test
	public void testProcess_ServerNoConnectionException() throws Exception {
		assertErrorMessage(v2Msg, MessageUtil.SERVER_NO_CONNECTION, new Processor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				throw new ServerNoConnectionException();
			}
			
		});
	}
	
	@Test
	public void testProcess_Hl7NoInputException() throws Exception {
		// This message won't make it to the endpoint so the assertion differs
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("CamelHttpResponseCode", 200);

		// Trigger route execution by sending input to route		
		Object out = template.requestBody("direct:sampleInput", "");
 
		// Verify response
		assertEquals(MessageUtil.HL7_ERROR_MSG_NO_INPUT_HL7, extractErrorMessage(out));
	}
	
	@Test
	public void testProcess_IllegalArgumentException() throws Exception {
		assertErrorMessage(v2Msg, MessageUtil.INVALID_PARAMETER, new Processor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				throw new IllegalArgumentException();
			}
			
		});
	}
	
	@Test
	public void testProcess_HttpOperationFailedException_400() throws Exception {
		String expectedErrorMsg = "Bad Request";
		Integer header = 400;
		assertErrorMessage(v2Msg, expectedErrorMsg, header);
	}
	
	@Test
	public void testProcess_HttpOperationFailedException_500() throws Exception {
		String expectedErrorMsg = "Internal Server Error";
		Integer header = 500;
		assertErrorMessage(v2Msg, expectedErrorMsg, header);
	}
	
	private void assertErrorMessage(String v2Message, String expectedErrorMsg, Integer statusCode) throws Exception {
		assertErrorMessage(v2Message, expectedErrorMsg, new Processor() {
			
			@Override
			public void process(Exchange exchange) throws Exception {
				throw new HttpOperationFailedException("", statusCode, expectedErrorMsg, "", null, "");
				
			}
		});
	}
	
	private void assertErrorMessage(String v2Message, String expectedErrorMsg, Processor processor) throws Exception {
		MockEndpoint mock = getMockEndpoint("mock:output");
		mock.expectedMessageCount(1);
		mock.whenAnyExchangeReceived(processor);

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("CamelHttpResponseCode", 200);

		// Trigger route execution by sending input to route		
		Object out = template.requestBody("direct:sampleInput", v2Message);
 
		// Verifies if input is equal to output
		assertMockEndpointsSatisfied(); 
		
		// Verify response from mock server
		assertEquals(expectedErrorMsg, extractErrorMessage(out));
	}
	
	private String extractErrorMessage(Object obj) {
		String str = (String) obj;
		String[] arr = str.split("\\|");
		return arr[14];
	}

}
