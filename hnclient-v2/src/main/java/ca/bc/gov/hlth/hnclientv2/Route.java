package ca.bc.gov.hlth.hnclientv2;

import java.io.File;
import java.security.KeyStore;
import java.time.LocalDate;

import ca.bc.gov.hlth.hnclientv2.authentication.RetrieveAccessToken;
import ca.bc.gov.hlth.hnclientv2.error.MessageUtil;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.hnclientv2.error.CamelCustomException;
import ca.bc.gov.hlth.hnclientv2.error.ErrorProcessor;
import ca.bc.gov.hlth.hnclientv2.error.FailureProcessor;
import ca.bc.gov.hlth.hnclientv2.authentication.ClientAuthenticationBuilder;
import ca.bc.gov.hlth.hnclientv2.authentication.ClientIdSecretBuilder;
import ca.bc.gov.hlth.hnclientv2.authentication.SignedJwtBuilder;
import ca.bc.gov.hlth.hnclientv2.handshakeserver.HandshakeServer;
import ca.bc.gov.hlth.hnclientv2.keystore.KeystoreTools;
import ca.bc.gov.hlth.hnclientv2.keystore.RenewKeys;
import ca.bc.gov.hlth.hnclientv2.wrapper.Base64Encoder;
import ca.bc.gov.hlth.hnclientv2.wrapper.ProcessV2ToJson;

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

    @EndpointInject("direct:start")
    ProducerTemplate producer;

    private static final String keystorePassword = System.getenv("MOH_HNCLIENT_KEYSTORE_PASSWORD");

    private RetrieveAccessToken retrieveAccessToken;

    /**
     * Camel route that:
     *   1. Receives a message over tcp using the HandShakeServer to implement a specific handshake protocol
     *   2. Retrieves a access token using Client Credential Grant
     *   3. Converts the message to base64 format
     *   4. Wraps the message in a JSON wrapper
     *   5. Sends the message to an http endpoint (HNS-ESB) with the JWT attached
     *   6. Returns the response to the original tcp caller
     */
    @Override
    public void configure() throws Exception {
        init();

        onException(org.apache.http.conn.HttpHostConnectException.class).process(new FailureProcessor(MessageUtil.SERVER_NO_CONNECTION))
        .log("Recieved body ${body}").handled(true);
        
        onException(CamelCustomException.class).process(new FailureProcessor(MessageUtil.UNKNOWN_EXCEPTION))
        .log("Recieved body ${body}").handled(true);
        
        onException(IllegalArgumentException.class).process(new FailureProcessor(MessageUtil.INVALID_PARAMETER))
        .log("Recieved body ${body}").handled(true);
        
        from("direct:start").routeId("hnclient-route")
            .log("Retrieving access token")
            .setHeader("Authorization").method(retrieveAccessToken).id("RetrieveAccessToken")
            // TODO we should refactor to standardize on beans or processors
            .setBody().method(new Base64Encoder())
            .process(new ProcessV2ToJson()).id("ProcessV2ToJson")
            .to("log:HttpLogger?level=DEBUG&showBody=true&showHeaders=true&multiline=true")
            .log("Sending to HNSecure")
            .to("{{http-protocol}}://{{hnsecure-hostname}}:{{hnsecure-port}}/{{hnsecure-endpoint}}?throwExceptionOnFailure=false").id("ToHnSecure")
            .log("Received response from HNSecure")
            // TODO we might be able to remove this processor and instead just set ?throwExceptionOnFailure=true in which case
            //  on org.apache.camel.common.HttpOperationFailedException will be thrown and could be handled by an onException handler
            .process(new ErrorProcessor())
            .to("log:HttpLogger?level=DEBUG&showBody=true&showHeaders=true&multiline=true")
            .convertBodyTo(String.class);
    }

    // This makes it easier to test the route and keeps some of this initialization code separate from the route config
    // TODO ideally this happens in the Constructor but @PropertyInject happens after the constructor so we call it from the route itself
    //  to call it from the constructor we could move property loading into MainMethod and pass the properties into the Constructor
    public void init() throws Exception {
        HandshakeServer handshakeServer = new HandshakeServer(producer);

        ClientAuthenticationBuilder clientAuthBuilder = getClientAuthentication();
        retrieveAccessToken = new RetrieveAccessToken(tokenEndpoint, scopes, clientAuthBuilder);
        if (clientAuthBuilder instanceof SignedJwtBuilder) {
            renewKeys();
        }
    }

    private ClientAuthenticationBuilder getClientAuthentication() throws Exception {
        if (clientAuthType.equals("SIGNED_JWT")) {
            return new SignedJwtBuilder(new File(jksFilePath), keyAlias, tokenEndpoint, keystorePassword);
        } else if (clientAuthType.equals("CLIENT_ID_SECRET")) {
            return new ClientIdSecretBuilder(clientId, System.getenv("MOH_HNCLIENT_SECRET"));
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
