package org.tidepool.keycloak.extensions.resource;

import io.prometheus.client.CollectorRegistry;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import javax.management.MalformedObjectNameException;
import java.io.File;
import java.io.IOException;

public class JmxMetricsEndpointFactory implements RealmResourceProviderFactory {

    private static final String DEFAULT_CONFIG_PATH = "/opt/jboss/keycloak/standalone/configuration/jmx_exporter.yaml";

    private JmxExporter exporter;

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        if (exporter == null) {
            throw new RuntimeException("Jmx exporter is not initialized");
        }
        return new JmxMetricsEndpoint(exporter);
    }

    @Override
    public void init(Config.Scope config) {
        String configPath = config.get("jmx-exporter-config", DEFAULT_CONFIG_PATH);
        File jmxExporterConfig = new File(configPath);
        try {
            exporter = new JmxExporter(jmxExporterConfig, new CollectorRegistry());
        } catch (IOException | MalformedObjectNameException e) {
            throw new RuntimeException("Failed to initialize jmx exporter", e);
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) { }

    @Override
    public void close() { }

    @Override
    public String getId() {
        return JmxMetricsEndpoint.ID;
    }
}
