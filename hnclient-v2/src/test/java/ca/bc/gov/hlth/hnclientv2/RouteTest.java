package ca.bc.gov.hlth.hnclientv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RouteTest extends CamelTestSupport {
	
	private static final String fhirJsonRequest = "{\"content\":[{\"attachment\":{\"data\":\"MDAwMDAzNTJNU0h8Xn5cJnxITldFQnxWSUhBfFJBSUdULVBSU04tRE1HUnxCQzAwMDAxMDEzfDIwMTcwMTI1MTIyMTI1fHRyYWluOTZ8UjAzfDIwMTcwMTI1MTIyMTI1fER8Mi40fHwKWkhEfDIwMTcwMTI1MTIyMTI1fF5eMDAwMDAwMTB8SE5BSUFETUlOSVNUUkFUSU9OfHx8fDIuNApQSUR8fDEyMzQ1Njc4OTBeXl5CQ15QSA==\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";
	
	private static final String fhirJsonResponse = "{\"content\":[{\"attachment\":{\"data\":\"TVNIfF5+XCZ8UkFJR1QtUFJTTi1ETUdSfEJDMDAwMDIwNDF8SE5XZWJ8QkMwMTAwMDAzMHwyMDIwMDIwNjEyMzg0MXx0cmFpbjk2fEU0NXwxODE5OTI0fER8Mi40Xk0KTVNBfEFBfDIwMjAwMjA2MTIzODQwfEhKTUIwMDFJU1VDQ0VTU0ZVTExZIENPTVBMRVRFRF5NCkVSUnxeXl5ISk1CMDAxSSZTVUNDRVNTRlVMTFkgQ09NUExFVEVEXk0KUElEfHwxMjM0NTY3ODleXl5CQ15QSF5NT0h8fHx8fDE5ODQwMjI1fE1eTQpaSUF8fHx8fHx8fHx8fHx8fHxMQVNUTkFNRV5GSVJTVF5TXl5eXkx8OTEyIFZJRVcgU1ReXl5eXl5eXl5eXl5eXl5eXl5eVklDVE9SSUFeQkNeVjhWM00yXkNBTl5IXl5eXk58XlBSTl5QSF5eXjI1MF4xMjM0NTY4\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}";

    private static final String v2Request = "00000352MSH|^~\\&|HNWEB|VIHA|RAIGT-PRSN-DMGR|BC00001013|20170125122125|train96|R03|20170125122125|D|2.4||\n"
            + "ZHD|20170125122125|^^00000010|HNAIADMINISTRATION||||2.4\n"
            + "PID||1234567890^^^BC^PH";
    
    private static final String v2Response = "MSH|^~\\&|RAIGT-PRSN-DMGR|BC00002041|HNWeb|BC01000030|20200206123841|train96|E45|1819924|D|2.4^M\n"
    		+ "MSA|AA|20200206123840|HJMB001ISUCCESSFULLY COMPLETED^M\n"
    		+ "ERR|^^^HJMB001I&SUCCESSFULLY COMPLETED^M\n"
    		+ "PID||123456789^^^BC^PH^MOH|||||19840225|M^M\n"
    		+ "ZIA|||||||||||||||LASTNAME^FIRST^S^^^^L|912 VIEW ST^^^^^^^^^^^^^^^^^^^VICTORIA^BC^V8V3M2^CAN^H^^^^N|^PRN^PH^^^250^1234568";

    @Produce("direct:start")
    private ProducerTemplate mockRouteStart;

    @EndpointInject("mock:hnsecure")
    private MockEndpoint hnSecureEndpoint;
    
    @EndpointInject("mock:output")
    private MockEndpoint outputEndpoint;
	
    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @BeforeEach
    public void configureRoutes() throws Exception {

        //Since we're not running from the main we need to set the properties
        PropertiesComponent pc = context.getPropertiesComponent();
        pc.setLocation("classpath:application.properties");
        String transactionId = new TransactionIdGenerator().generateUuid();

        // Skip the init() steps
        Route routeMock = spy(Route.class);
        doNothing().when(routeMock).init(transactionId);

        context.addRoutes(routeMock);
        // Skip the retrieve access token step and the send to HnSecure step
        AdviceWithRouteBuilder.adviceWith(context, "hnclient-route", a -> {
            a.replaceFromWith("direct:start");
            a.weaveById("RetrieveAccessToken").replace().to("mock:RetrieveAccessToken");
            a.weaveById("ToHnSecure").replace().to("mock:hnsecure");
            a.weaveAddLast().to("mock:output");
        });
    }

    @Test
    public void testSuccessfulMessage() throws Exception {

        context.start();

        // We expect a single base64 encoded message like HnSecure would get
        hnSecureEndpoint.expectedMessageCount(1);
        hnSecureEndpoint.expectedBodiesReceived(fhirJsonRequest);
        
        hnSecureEndpoint.whenAnyExchangeReceived(new Processor() {
			
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getMessage().setBody(fhirJsonResponse);
			}
		});

        // Send a message (include a 200 response code so it passes the error processor)
        mockRouteStart.sendBodyAndHeader(v2Request, Exchange.HTTP_RESPONSE_CODE, 200);

        // Verify our expectations were met
        assertMockEndpointsSatisfied();

        // Verify that the response is converted from FHIR JSON to HL7v2
        assertEquals(v2Response, outputEndpoint.getExchanges().get(0).getIn().getBody());

        context.stop();
    }
}
