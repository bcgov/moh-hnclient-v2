package ca.bc.gov.hlth.hnclientv2.authentication;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;

public class ClientIdSecretBuilderTest {

    @Test
    public void newClientIdSecretBuilder_nullClientIdTest() {
        // When I initialize a new ClientIdSecretBuilder with a null client id
        // expect a null pointer
    	assertThrows(NullPointerException.class, () -> {
    		new ClientIdSecretBuilder(null, "test");	
    	});        
    }

    @Test
    public void newClientIdSecretBuilder_nullSecretTest() {
        // When I initialize a new ClientIdSecretBuilder without a secret set in the system environment variables
        // expect a null pointer
    	assertThrows(NullPointerException.class, () -> {
    		new ClientIdSecretBuilder("test", null);
    	});
    }

    @Test
    public void buildClientIdSecretBuilderTest() {
        // When I build a new ClientIdSecretBuilder
        // Expect that I get a ClientAuthentication object of type ClientSecretBasic
        ClientAuthenticationBuilder clientAuthenticationBuilder = new ClientIdSecretBuilder("test", "test");
        ClientAuthentication clientAuthentication = clientAuthenticationBuilder.buildAuthenticationMethod();

        assertTrue(clientAuthentication instanceof ClientSecretBasic);
    }
}
