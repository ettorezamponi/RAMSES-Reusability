package tools.ezamponi;

import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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
        new DiskSpaceMetrics(new File("/")).bindTo(prometheusRegistry);
    }

    @SuppressWarnings("checkstyle:designforextension")
    @Produces(MediaType.TEXT_PLAIN)
    public static String getMetrics(UriInfo uriInfo) {
        MicrometerResource();

        String melodyPath = convertUri(uriInfo);

        String melodyMetrics = fetchExternalMetrics(melodyPath);
        return prometheusRegistry.scrape() + "\n" + melodyMetrics;
    }

    private static String convertUri (UriInfo uriInfo){
        String path = uriInfo.getBaseUri().toString()
                .replace("rest/","monitoring?format=prometheus&includeLastValue=true"); // "http://localhost:8080/tools.descartes.teastore.webui/rest/"
        System.out.println("For the service "+uriInfo.getBaseUri()+" the MELODY PATH is "+path);
        return path;
    }

    private static String fetchExternalMetrics(String url) {
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(url).request().get();

            if (response.getStatus() == 200) {
                return response.readEntity(String.class);
            } else {
                LOG.error("Error during javamelody response with error: {}", response.getStatus());
            }
        } catch (Exception e) {
            LOG.error("Error during metrics exporting: ", e);
        }

        return "Error during the try-catch block";
    }
}
