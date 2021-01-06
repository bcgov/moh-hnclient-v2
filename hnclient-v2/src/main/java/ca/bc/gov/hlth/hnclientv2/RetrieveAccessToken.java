package ca.bc.gov.hlth.hnclientv2;

import ca.bc.gov.hlth.hnclientv2.auth.ClientAuthenticationBuilder;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Objects;

public class RetrieveAccessToken {

    private static Logger logger = LoggerFactory.getLogger(RetrieveAccessToken.class);

    private URI tokenEndpointUri;
    private Scope requiredScopes;
    private ClientAuthenticationBuilder clientAuthBuilder;

    private AccessToken accessToken;
    private long tokenExpiryTime;

    public RetrieveAccessToken(String tokenEndpoint, String scope, ClientAuthenticationBuilder clientAuthBuilder) throws URISyntaxException {

        //TODO this accepts a string array or multiple strings - will need to update to allow multiple scopes
        this.requiredScopes = new Scope(scope);
        this.tokenEndpointUri = new URI(tokenEndpoint);
        this.clientAuthBuilder = clientAuthBuilder;
        Objects.requireNonNull(this.clientAuthBuilder, "Requires client authentication.");
    }

    public synchronized String getToken() throws Exception {

        // Reuse the token if the expiry time is more than a minute away
        if (Instant.now().toEpochMilli() + 60_000 < tokenExpiryTime) {
            logger.info(String.format("Using existing access token"));
            logger.debug(String.format("Access token: %s", accessToken.toJSONString()));
            return accessToken.toAuthorizationHeader();
        }

        // Construct the client credentials grant type
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        // Get the client authentication method
        ClientAuthentication clientAuthentication = clientAuthBuilder.build();

        // Make the token request
        TokenRequest request = new TokenRequest(tokenEndpointUri, clientAuthentication, clientGrant, requiredScopes);

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());
        if (!response.indicatesSuccess()) {
            // TODO - handle this error
            TokenErrorResponse errorResponse = response.toErrorResponse();
            throw new IllegalStateException(errorResponse.toJSONObject().toString());
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        // Get the access token and set the expiry time
        accessToken = successResponse.getTokens().getAccessToken();
        // This could be off by a few seconds because it doesn't account for network latency getting the token
        tokenExpiryTime = Instant.now().toEpochMilli() + (accessToken.getLifetime() * 1000);

        logger.debug(String.format("Access token: %s", accessToken.toJSONString()));
        logger.info(String.format("Token Expires at: %s", tokenExpiryTime));

        return accessToken.toAuthorizationHeader();
    }
}