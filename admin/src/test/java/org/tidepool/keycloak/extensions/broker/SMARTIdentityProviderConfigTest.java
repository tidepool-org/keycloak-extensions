package org.tidepool.keycloak.extensions.broker;

import org.junit.jupiter.api.Test;
import org.keycloak.models.IdentityProviderModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The stored client secret may only be reused for a masked admin update when the secret would still
 * be sent to the same place (Keycloak 26.8.1, CVE-2026-15943).
 */
class SMARTIdentityProviderConfigTest {

    private static final String ISSUER = "https://fhir.example-ehr.org/r4";
    private static final String CLIENT_ID = "tidepool";

    @Test
    void allowsReuseWhenIssuerAndClientIdAreUnchanged() {
        SMARTIdentityProviderConfig update = config(ISSUER, CLIENT_ID);
        update.setScopes("openid fhirUser launch");

        assertThat(update.canReuseMaskedClientSecret(config(ISSUER, CLIENT_ID))).isTrue();
    }

    @Test
    void rejectsReuseWhenIssuerChanges() {
        SMARTIdentityProviderConfig update = config("https://attacker.example.com/r4", CLIENT_ID);

        assertThat(update.canReuseMaskedClientSecret(config(ISSUER, CLIENT_ID))).isFalse();
    }

    @Test
    void rejectsReuseWhenClientIdChanges() {
        SMARTIdentityProviderConfig update = config(ISSUER, "someone-else");

        assertThat(update.canReuseMaskedClientSecret(config(ISSUER, CLIENT_ID))).isFalse();
    }

    @Test
    void treatsTrailingSlashOnIssuerAsTheSameDestination() {
        SMARTIdentityProviderConfig update = config(ISSUER + "/", CLIENT_ID);

        assertThat(update.canReuseMaskedClientSecret(config(ISSUER, CLIENT_ID))).isTrue();
    }

    @Test
    void treatsMissingAndBlankValuesAsEqual() {
        SMARTIdentityProviderConfig update = config("", null);

        assertThat(update.canReuseMaskedClientSecret(config(null, " "))).isTrue();
    }

    @Test
    void comparesAgainstAPlainStoredModel() {
        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("issuer", ISSUER);
        stored.getConfig().put("clientId", CLIENT_ID);

        assertThat(config(ISSUER, CLIENT_ID).canReuseMaskedClientSecret(stored)).isTrue();
        assertThat(config("https://attacker.example.com/r4", CLIENT_ID).canReuseMaskedClientSecret(stored)).isFalse();
    }

    @Test
    void rejectsReuseWithoutAStoredModel() {
        assertThat(config(ISSUER, CLIENT_ID).canReuseMaskedClientSecret(null)).isFalse();
    }

    private static SMARTIdentityProviderConfig config(String issuer, String clientId) {
        SMARTIdentityProviderConfig config = new SMARTIdentityProviderConfig();
        config.setIssuer(issuer);
        config.setClientId(clientId);
        return config;
    }
}
