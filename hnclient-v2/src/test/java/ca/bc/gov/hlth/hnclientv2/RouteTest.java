package ca.bc.gov.hlth.hnclientv2;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class RouteTest  extends CamelTestSupport {

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Produce("direct:start")
    private ProducerTemplate mockRouteStart;

    @EndpointInject("mock:response")
    private MockEndpoint responseEndpoint;


    @Before
    public void configureRoutes() throws Exception {

        //Since we're not running from the main we need to set the properties
        PropertiesComponent pc = context.getPropertiesComponent();
        pc.setLocation("classpath:application.properties");

        // Skip the init() steps
        Route routeMock = spy(Route.class);
        doNothing().when(routeMock).init();

        context.addRoutes(routeMock);
        // Skip the retrieve access token step and the send to HnSecure step
        AdviceWithRouteBuilder.adviceWith(context, "hnclient-route", a -> {
            a.replaceFromWith("direct:start");
            a.weaveById("RetrieveAccessToken").replace().to("mock:RetrieveAccessToken");
            a.weaveById("ToHnSecure").replace().to("mock:hnsecure");
            a.weaveAddLast().to("mock:response");
        });
    }

    @Test
    public void testSuccessfulMessage() throws Exception {

        context.start();

        // We expect a single base64 encoded message like HnSecure would get
        getMockEndpoint("mock:response").expectedMessageCount(1);
        responseEndpoint.expectedBodiesReceived("{\"content\":[{\"attachment\":{\"data\":\"dGVzdA==\",\"contentType\":\"x-application\\/hl7-v2+er7\"}}],\"resourceType\":\"DocumentReference\",\"status\":\"current\"}");

        // Send a message (include a 200 response code so it passes the error processor)
        mockRouteStart.sendBodyAndHeader("test", Exchange.HTTP_RESPONSE_CODE, 200);

        // Verify our expectations were met
        assertMockEndpointsSatisfied();

        context.stop();
    }
}
