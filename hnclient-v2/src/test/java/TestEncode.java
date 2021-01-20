
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import ca.bc.gov.hlth.hnclientv2.Base64Encoder;

public class TestEncode extends CamelTestSupport{
	
	private String v2Msg = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
            + "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
            + "PID||1234567890^^^BC^PH";
	
	@Override
	protected RouteBuilder createRouteBuilder() throws Exception{
		
		return new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {			
				from("direct:sampleInput")               
                .setBody().method(new Base64Encoder())
                .log("v2Message encoded to Base64 format")
                .log("Sending to HNSecure")
                .to("mock:output");
				
			}
		};
	}
	
	@Test
	public void testEncodeV2Message() throws Exception {
		
		MockEndpoint mock = getMockEndpoint("mock:output");
		mock.expectedMessageCount(1);
		
		mock.expectedBodiesReceived(new Base64Encoder().encodeString(v2Msg)); //Setting expected output to mocked endpoint
	    
	    template.sendBody("direct:sampleInput",v2Msg );//triggering route execution by sending input to route
	    assertMockEndpointsSatisfied(); //Verifies if input is equal to output

	}
	
	
}
