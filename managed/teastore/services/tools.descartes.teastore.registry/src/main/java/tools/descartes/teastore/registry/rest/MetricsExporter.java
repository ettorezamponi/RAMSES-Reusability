package tools.descartes.teastore.registry.rest;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO make it reachable through "/actuator/prometheus" and not tools.descartes.ecc
@Path("/prometheus")
public class MetricsExporter {
    private static final Logger LOG = LoggerFactory.getLogger(RegistryStartup.class);
    PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    public void MicrometerResource() {
        new ClassLoaderMetrics().bindTo(prometheusRegistry);
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        new JvmGcMetrics().bindTo(prometheusRegistry);
        new ProcessorMetrics().bindTo(prometheusRegistry);
        new JvmThreadMetrics().bindTo(prometheusRegistry);

    }


    @SuppressWarnings("checkstyle:designforextension")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMetrics() {
        MicrometerResource();
        return prometheusRegistry.scrape();
    }

}
