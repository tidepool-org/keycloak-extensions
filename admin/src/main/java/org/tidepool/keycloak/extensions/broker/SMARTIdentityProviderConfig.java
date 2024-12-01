package org.tidepool.keycloak.extensions.broker;

import org.keycloak.models.IdentityProviderModel;

public class SMARTIdentityProviderConfig extends IdentityProviderModel  {

    public SMARTIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public SMARTIdentityProviderConfig() {
        super();
    }

    public String getIssuer() {
        return getConfig().get("issuer");
    }

    public void setIssuer(String issuer) {
        getConfig().put("issuer", issuer);
    }

    public String getClientId() {
        return getConfig().get("clientId");
    }

    public void setClientId(String clientId) {
        getConfig().put("clientId", clientId);
    }

    public String getClientSecret() {
        return getConfig().get("clientSecret");
    }

    public void setClientSecret(String clientSecret) {
        getConfig().put("clientSecret", clientSecret);
    }

    public String getScopes() {
        return getConfig().get("scopes");
    }

    public void setScopes(String scopes) {
        getConfig().put("scopes", scopes);
    }

    public String getForwardParameters() {
        return getConfig().get("forwardParameters");
    }

    public void setForwardParameters(String params) {
        getConfig().put("forwardParameters", params);
    }

    public String getFHIRVersion() {
        return getConfig().get("fhirVersion");
    }

    public void setFHIRVersion(String version) {
        getConfig().put("fhirVersion", version);
    }
}
