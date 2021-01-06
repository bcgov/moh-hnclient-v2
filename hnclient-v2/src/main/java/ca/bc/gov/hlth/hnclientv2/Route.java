package ca.bc.gov.hlth.hnclientv2;

import ca.bc.gov.hlth.hnclientv2.auth.ClientAuthenticationBuilder;
import ca.bc.gov.hlth.hnclientv2.auth.ClientIdSecretBuilder;
import ca.bc.gov.hlth.hnclientv2.auth.SignedJwtBuilder;
import ca.bc.gov.hlth.hnclientv2.keystore.KeystoreTools;
import ca.bc.gov.hlth.hnclientv2.keystore.RenewKeys;
import io.netty.buffer.ByteBuf;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.KeyStore;
import java.time.LocalDate;

public class Route extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(Route.class);

    @PropertyInject(value = "token-endpoint")
    String tokenEndpoint;

    @PropertyInject(value = "client-id")
    String clientId;

    @PropertyInject(value = "scopes")
    String scopes;

    @PropertyInject(value = "client-auth-type")
    String clientAuthType;

    @PropertyInject(value = "jks-file")
    private String jksFilePath;

    @PropertyInject(value = "jks-key-alias")
    private String keyAlias;

    @PropertyInject(value = "cer-file")
    private String cerFilePath;

    @PropertyInject(value = "cert-upload-endpoint")
    private String cerUploadEndpoint;

    private static final String keystorePassword = System.getenv("MOH_HNCLIENT_KEYSTORE_PASSWORD");

    private RetrieveAccessToken retrieveAccessToken;

    /**
     * Camel route that:
     *   1. Receives a message over tcp
     *   2. Retrieves a access token using Client Credential Grant
     *   3. Passes the message to an http endpoint with the JWT attached
     *   4. Returns the response
     */
    @Override
    public void configure() throws Exception {

        ClientAuthenticationBuilder clientAuthBuilder = getClientAuthentication();
        retrieveAccessToken = new RetrieveAccessToken(tokenEndpoint, scopes, clientAuthBuilder);
        // TODO this might be better to just be run from main but requires a property loader and modifying the retrieveAccessToken
        renewKeys();

        from("netty:tcp://{{hostname}}:{{port}}")
                .log("Retrieving access token")
                .setHeader("Authorization").method(retrieveAccessToken)
                .to("log:HttpLogger?level=DEBUG&showBody=true&showHeaders=true&multiline=true")
                .log("Sending to HNSecure")
                .to("http://{{hnsecure-hostname}}:{{hnsecure-port}}/{{hnsecure-endpoint}}?throwExceptionOnFailure=false")
                .log("Received response from HNSecure")
                .convertBodyTo(String.class)
                .to("log:HttpLogger?level=DEBUG&showBody=true&showHeaders=true&multiline=true")
                .convertBodyTo(ByteBuf.class);
    }

    private ClientAuthenticationBuilder getClientAuthentication() throws Exception {
        if (clientAuthType.equals("SIGNED_JWT")) {
            return new SignedJwtBuilder(new File(jksFilePath), keyAlias, tokenEndpoint, keystorePassword);
        } else if (clientAuthType.equals("CLIENT_ID_SECRET")) {
            return new ClientIdSecretBuilder(clientId);
        } else {
            throw new IllegalStateException(String.format("Unrecognized client authentication type: '%s'", clientAuthType));
        }
    }

    private void renewKeys() throws Exception {
        // Check the existing keys to see if they should be renewed
        File jksFile = new File(jksFilePath);
        // TODO this might need to be configurable
        int daysBeforeExpiryToRenew = 30;

        KeyStore keystore = KeystoreTools.loadKeyStore(jksFile, keystorePassword, "jks");
        LocalDate certExpiry = KeystoreTools.getKeystoreExpiry(keystore, keyAlias);
        LocalDate dateRangeToRenewCert = LocalDate.now().plusDays(daysBeforeExpiryToRenew);
        if (!certExpiry.isBefore(dateRangeToRenewCert)) {
            logger.info("Certificates do not expire before " + dateRangeToRenewCert + ". Certificates will not be renewed");
        } else {
            //This may actually need a different instance of RetrieveAccessToken with different scopes
            logger.info("Certificates expire before " + dateRangeToRenewCert + ". Certificates will be renewed");
            RenewKeys.renewKeys(retrieveAccessToken.getToken(), 1, keyAlias, jksFilePath, cerFilePath, cerUploadEndpoint, keystorePassword);

            //Reset the client auth builder with the new cert
            ClientAuthenticationBuilder clientAuthBuilder = getClientAuthentication();
            retrieveAccessToken = new RetrieveAccessToken(tokenEndpoint, scopes, clientAuthBuilder);
        }
    }
}
