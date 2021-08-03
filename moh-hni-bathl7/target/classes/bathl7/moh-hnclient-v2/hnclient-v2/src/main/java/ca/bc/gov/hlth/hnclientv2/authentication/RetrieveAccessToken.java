package ca.bc.gov.hlth.hnclientv2.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import ca.bc.gov.hlth.hnclientv2.error.CamelCustomException;
import ca.bc.gov.hlth.hnclientv2.error.ServerNoConnectionException;
import ca.bc.gov.hlth.hncommon.util.LoggingUtil;
import io.netty.util.internal.StringUtil;

public class RetrieveAccessToken {

    private static final Logger logger = LoggerFactory.getLogger(RetrieveAccessToken.class);
    
    private static final Long ONE_MINUTE_IN_MILLIS = 60_000L;

    private URI tokenEndpointUri;
    private Scope requiredScopes;
    private ClientAuthenticationBuilder clientAuthBuilder;

    private AccessToken accessToken;
    private long tokenExpiryTime;
  
    public RetrieveAccessToken(String tokenEndpoint, String scopes, ClientAuthenticationBuilder clientAuthBuilder) throws URISyntaxException {
    	if (!StringUtil.isNullOrEmpty(scopes)) {
            this.requiredScopes = new Scope(scopes.split(" "));	
    	} else {
    		this.requiredScopes = new Scope();
    	}
        this.tokenEndpointUri = new URI(tokenEndpoint);
        this.clientAuthBuilder = clientAuthBuilder;      
        Objects.requireNonNull(this.clientAuthBuilder, "Requires client authentication.");
    }
   
    public synchronized String getToken() throws CamelCustomException {
    	String methodName = LoggingUtil.getMethodName();

        // Reuse the token if the expiry time is more than a minute away
        if (Instant.now().toEpochMilli() + ONE_MINUTE_IN_MILLIS < tokenExpiryTime) {
            logger.info("{} - Using existing access token", methodName);
            logger.debug("{} - Access token: {}", accessToken.toJSONString(), methodName);
            return accessToken.toAuthorizationHeader();
        }

        // Construct the client credentials grant type
        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        // Get the client authentication method
        ClientAuthentication clientAuthentication = clientAuthBuilder.build();

        // Make the token request
        TokenRequest request = new TokenRequest(tokenEndpointUri, clientAuthentication, clientGrant, requiredScopes);

        TokenResponse response;
        try {
            response = TokenResponse.parse(request.toHTTPRequest().send());
        } catch (ParseException | IOException exception) {
            throw new ServerNoConnectionException(exception.getMessage());
        }

        // Check if we got a 2xx response back from the server
        if (!response.indicatesSuccess()) {
            TokenErrorResponse errorResponse = response.toErrorResponse();
            throw new ServerNoConnectionException(errorResponse.toJSONObject().toString());
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        // Get the access token and set the expiry time
        accessToken = successResponse.getTokens().getAccessToken();
        // This could be off by a few seconds because it doesn't account for network latency getting the token
        tokenExpiryTime = Instant.now().toEpochMilli() + (accessToken.getLifetime() * 1000);

        logger.debug("{} -  Access token: {}", methodName, accessToken.toJSONString());
        logger.info("{} - Token Expires at: {}",methodName, tokenExpiryTime);

        return accessToken.toAuthorizationHeader();
    }
}