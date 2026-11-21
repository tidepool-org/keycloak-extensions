package org.tidepool.keycloak.extensions.broker;

import org.keycloak.models.IdentityProviderModel;

import java.util.Map;
import java.util.Objects;

public class SMARTIdentityProviderConfig extends IdentityProviderModel  {

    static final String ISSUER = "issuer";
    static final String CLIENT_ID = "clientId";

    public SMARTIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public SMARTIdentityProviderConfig() {
        super();
    }

    public String getIssuer() {
        return getConfig().get(ISSUER);
    }

    public void setIssuer(String issuer) {
        getConfig().put(ISSUER, issuer);
    }

    public String getClientId() {
        return getConfig().get(CLIENT_ID);
    }

    public void setClientId(String clientId) {
        getConfig().put(CLIENT_ID, clientId);
    }

    /**
     * Keycloak 26.8.1 (CVE-2026-15943) keeps the stored client secret on an admin update that sends
     * back the masked value only if the provider says the secret's destination is unchanged.
     * <p>
     * SMART ignores any stored token URL: it discovers the token endpoint at runtime from
     * {@code <issuer>/.well-known/smart-configuration} (see {@link SMARTIdentityProvider}). So the
     * secret's destination is determined by the issuer, and the credential by the client id. If either
     * changes, the caller must supply the real secret instead of the mask, otherwise someone with
     * {@code manage-identity-providers} could point the issuer at their own server and receive it.
     */
    @Override
    public boolean canReuseMaskedClientSecret(IdentityProviderModel other) {
        if (other == null) {
            return false;
        }
        Map<String, String> otherConfig = other.getConfig() == null ? Map.of() : other.getConfig();
        return Objects.equals(normalizeIssuer(getIssuer()), normalizeIssuer(otherConfig.get(ISSUER)))
                && Objects.equals(normalize(getClientId()), normalize(otherConfig.get(CLIENT_ID)));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Discovery appends a trailing slash when missing, so {@code https://x} and {@code https://x/} are the same. */
    private static String normalizeIssuer(String issuer) {
        String normalized = normalize(issuer);
        return normalized != null && normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
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
