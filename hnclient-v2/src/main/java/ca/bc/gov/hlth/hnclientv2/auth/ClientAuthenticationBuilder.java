package ca.bc.gov.hlth.hnclientv2.auth;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

public interface ClientAuthenticationBuilder {

    ClientAuthentication build();
}
