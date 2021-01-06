package ca.bc.gov.hlth.hnclientv2.auth;

import ca.bc.gov.hlth.hnclientv2.keystore.KeystoreTools;
import ca.bc.gov.hlth.hnclientv2.Util;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.util.Objects;

/**
 * Builds a client authentication function capable of retrieving access tokens using a Signed JWT.
 *
 * It depends on an environment variable named "MOH_HNCLIENT_KEYSTORE_PASSWORD" which holds the password to the given
 * JKS file and key.
 *
 * This client authentication function corresponds to the "private_key_jwt" method in the OIDC specification, and
 * "Signed JWT" in the Keycloak documentation:
 *
 *   - https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication
 *   - https://www.keycloak.org/docs/latest/securing_apps/index.html#client-id-and-client-secret
 *   - https://www.keycloak.org/docs/latest/server_admin/index.html#_client-credentials
 *
 * Note that this is not the same as "client_secret_jwt", also known as "Signed JWT with Client Secret".
 */
public class SignedJwtBuilder implements ClientAuthenticationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SignedJwtBuilder.class);

    private final String keyAlias;
    private final URI tokenEndpoint;
    private final RSAPrivateKey privateKey;

    public SignedJwtBuilder(File keystoreFile, String keyAlias, String tokenEndpoint, String keystorePassword) throws Exception {
        Objects.requireNonNull(keystoreFile, "Requires keystore file.");
        Util.requireNonBlank(keyAlias, "Requires key alias.");
        Util.requireNonBlank(tokenEndpoint, "Requires token endpoint.");
        Objects.requireNonNull(keystorePassword, "Requires keystore password.");

        this.keyAlias = keyAlias;
        this.tokenEndpoint = new URI(tokenEndpoint);

        KeyStore keyStore = KeystoreTools.loadKeyStore(keystoreFile, keystorePassword, "JKS");
        privateKey = (RSAPrivateKey) keyStore.getKey(this.keyAlias, keystorePassword.toCharArray());
    }

    @Override
    public ClientAuthentication build() {
        logger.info("Building signed JWT.");
        try {
            ClientID clientID = new ClientID(keyAlias);
            return new PrivateKeyJWT(clientID, tokenEndpoint, JWSAlgorithm.RS256, privateKey, null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
