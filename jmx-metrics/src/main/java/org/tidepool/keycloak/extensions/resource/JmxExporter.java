package org.tidepool.keycloak.extensions.resource;

import javax.management.MalformedObjectNameException;
import java.io.*;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.jmx.JmxCollector;
import io.prometheus.jmx.BuildInfoCollector;

public class JmxExporter {

    private final CollectorRegistry registry;

    public JmxExporter(File config, CollectorRegistry registry) throws IOException, MalformedObjectNameException {
        this.registry = registry;
        new BuildInfoCollector().register(registry);
        new JmxCollector(config).register(registry);
    }

    public void export(final OutputStream stream) throws IOException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter(stream));
        TextFormat.write004(writer, registry.metricFamilySamples());
        writer.flush();
    }

}
