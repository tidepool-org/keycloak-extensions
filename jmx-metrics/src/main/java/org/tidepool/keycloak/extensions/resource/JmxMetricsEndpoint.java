package org.tidepool.keycloak.extensions.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import org.keycloak.services.resource.RealmResourceProvider;

public class JmxMetricsEndpoint implements RealmResourceProvider {

    public final static String ID = "jmx-metrics";

    private final JmxExporter exporter;

    public JmxMetricsEndpoint(JmxExporter exporter) {
        this.exporter = exporter;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get() {
        final StreamingOutput stream = exporter::export;
        return Response.ok(stream).build();
    }

    @Override
    public void close() { }
}
