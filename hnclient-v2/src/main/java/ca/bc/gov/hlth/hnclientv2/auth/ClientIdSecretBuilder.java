package ca.bc.gov.hlth.hnclientv2.auth;

import ca.bc.gov.hlth.hnclientv2.Util;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Builds a client authentication function capable of retrieving access tokens using a Client ID and a Client Secret.
 *
 *  It depends on an environment variable named "MOH_HNCLIENT_SECRET" which holds the client secret.
 *
 * Corresponds to the "client_secret_post" method in the OIDC specification, and "Client ID and Client Secret" method
 * in the Keycloak documentation:
 *
 *   - https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
 *   - https://www.keycloak.org/docs/latest/securing_apps/index.html#client-id-and-client-secret
 *   - https://www.keycloak.org/docs/latest/server_admin/index.html#_client-credentials
 */
public class ClientIdSecretBuilder implements ClientAuthenticationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClientIdSecretBuilder.class);

    private final String clientId;
    private final String clientSecretEnv;

    public ClientIdSecretBuilder(String clientId) {
        this.clientId = clientId;
        clientSecretEnv = System.getenv("MOH_HNCLIENT_SECRET");

        Util.requireNonBlank(this.clientId, "Requires client ID.");
        Objects.requireNonNull(this.clientSecretEnv, "Requires client secret password.");
    }

    @Override
    public ClientAuthentication build() {
        logger.info("Building client ID and secret.");
        // The credentials to authenticate the client at the token endpoint
        ClientID clientID = new ClientID(clientId);
        Secret clientSecret = new Secret(clientSecretEnv);
        return new ClientSecretBasic(clientID, clientSecret);
    }
}
