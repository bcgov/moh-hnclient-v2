package ca.bc.gov.hlth.hnclientv2.authentication;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

public interface ClientAuthenticationBuilder {

    ClientAuthentication buildAuthenticationMethod();
}
