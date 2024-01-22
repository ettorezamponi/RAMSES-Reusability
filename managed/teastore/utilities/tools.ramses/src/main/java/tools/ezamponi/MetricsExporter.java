package tools.ezamponi;

import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO make it reachable through "/actuator/prometheus" and not tools.descartes.ecc
public class MetricsExporter {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsExporter.class);
    static PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);


    public static void MicrometerResource() {
        new ClassLoaderMetrics().bindTo(prometheusRegistry);
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        new JvmGcMetrics().bindTo(prometheusRegistry);
        new ProcessorMetrics().bindTo(prometheusRegistry);
        new JvmThreadMetrics().bindTo(prometheusRegistry);

    }

    @SuppressWarnings("checkstyle:designforextension")
    @Produces(MediaType.TEXT_PLAIN)
    public static String getMetrics() {
        MicrometerResource();
        return prometheusRegistry.scrape();
    }

}
