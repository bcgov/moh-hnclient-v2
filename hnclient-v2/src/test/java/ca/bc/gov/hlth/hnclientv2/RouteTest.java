/**
 * 
 */
package ca.bc.gov.hlth.hnclientv2;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import ca.bc.gov.hlth.hnclientv2.json.ProcessV2ToJson;

/**
 * @author Tony.Ma * 
 * @date Jan. 20, 2021
 *
 */
public class RouteTest extends CamelTestSupport {
	
	String TestV2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
			+ "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
			+ "PID||1234567890^^^BC^PH";
	
	String encodeV2Msg = "MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==";
	
	
	@Override
	protected RouteBuilder createRouteBuilder() throws Exception{

		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {			
				from("direct:sampleInput")
				.process(new ProcessV2ToJson())
                .log("v2Message decode")
                .log("Sending to HNClient")
                .to("mock:output");

			}
		};
	}
	
	
	@Test
	public void testV2MsgFlow() throws Exception {
		String expectedMsg = "{\"content\":[{\"attachment\":{\"data\":\"MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";
		MockEndpoint mock = getMockEndpoint("mock:output");
		mock.expectedMessageCount(1);
		mock.expectedBodiesReceived(expectedMsg);		
	    template.sendBody("direct:sampleInput",encodeV2Msg );	
	    assertMockEndpointsSatisfied();
	}
	
	
}
