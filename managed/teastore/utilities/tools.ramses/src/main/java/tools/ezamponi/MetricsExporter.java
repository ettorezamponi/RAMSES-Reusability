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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
        String metrics = prometheusRegistry.scrape() + "\n" + fetchExternalMetrics(uriInfo);

        return metrics;
    }

    private static String convertUri (UriInfo uriInfo){
        // I received the old port to change
        String baseUri = uriInfo.getBaseUri().toString();
        URI uri = null;
        String oldPort;
        try {
            uri = new URI(baseUri);
            oldPort = String.valueOf(uri.getPort());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // I managed to reach the correct url to pass to fetching method
        String path = uriInfo.getBaseUri().toString()
                .replaceFirst("rest/","monitoring?format=prometheus&includeLastValue=true&includeLastValue=true") // "http://localhost:8080/tools.descartes.teastore.webui/rest/"
                .replace(oldPort, "8080");
        System.out.println("For the service "+uriInfo.getBaseUri()+" the MELODY PATH to give to probe is:\n"+path);
        return path;
    }

    public static String fetchExternalMetrics(UriInfo uriInfo) {
        //TODO aggiungi il blocco try-catch
        String url = convertUri(uriInfo);

        Client client = ClientBuilder.newClient();
        Response response = client.target(url).request().get();

        if (response.getStatus() == 200) {
            return response.readEntity(String.class);
        } else {
            LOG.error("Error during javamelody response with error: {}", response.getStatus());
            return "ERROR WITH MELODY METRICS";
        }
    }

}
