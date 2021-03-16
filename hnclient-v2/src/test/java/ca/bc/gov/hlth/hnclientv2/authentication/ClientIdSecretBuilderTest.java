package ca.bc.gov.hlth.hnclientv2.authentication;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ClientIdSecretBuilderTest {

    @Test(expected = NullPointerException.class)
    public void newClientIdSecretBuilder_nullClientIdTest() {
        // When I initialize a new ClientIdSecretBuilder with a null client id
        // expect a null pointer
        ClientAuthenticationBuilder clientAuthenticationBuilder = new ClientIdSecretBuilder(null, "test");
    }

    @Test(expected = NullPointerException.class)
    public void newClientIdSecretBuilder_nullSecretTest() {
        // When I initialize a new ClientIdSecretBuilder without a secret set in the system environment variables
        // expect a null pointer
        ClientAuthenticationBuilder clientAuthenticationBuilder = new ClientIdSecretBuilder("test", null);
    }

    @Test
    public void buildClientIdSecretBuilderTest() {
        // When I build a new ClientIdSecretBuilder
        // Expect that I get a ClientAuthentication object of type ClientSecretBasic
        ClientAuthenticationBuilder clientAuthenticationBuilder = new ClientIdSecretBuilder("test", "test");
        ClientAuthentication clientAuthentication = clientAuthenticationBuilder.build();

        assertTrue(clientAuthentication instanceof ClientSecretBasic);
    }
}
