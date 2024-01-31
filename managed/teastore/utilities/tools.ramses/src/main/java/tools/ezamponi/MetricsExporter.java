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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
        String melodyMetrics = fetchExternalMetrics(convertUri(uriInfo));
        return prometheusRegistry.scrape() + "\n" + melodyMetrics;
    }

    private static String convertUri (UriInfo uriInfo){
        String path = uriInfo.getBaseUri().toString()
                .replace("rest/","monitoring?format=prometheus&includeLastValue=true"); // "http://localhost:8080/tools.descartes.teastore.webui/rest/"
        System.out.println("For the service "+uriInfo.getBaseUri()+" the MELODY PATH is "+path);
        return path;
    }

    private static String fetchExternalMetrics(String url) {
        //TODO aggiungi il blocco try-catch
        Client client = ClientBuilder.newClient();
        Response response = client.target(url).request().get();

        if (response.getStatus() == 200) {
            System.out.println("RESPONSE 200, EXTERNAL MELODY METRICS FETCHED");
            return response.readEntity(String.class);
        } else {
            LOG.error("Error during javamelody response with error: {}", response.getStatus());
            return "ERROR WITH MELODY METRICS";
        }
    }

   /* public static String fetchExternalMetrics(String url) {
        try {
            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();

            int statusCode = connection.getResponseCode();

            if (statusCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                return response.toString();
            } else {
                return "HTTP request failed with status code: " + statusCode;
            }
        } catch (Exception e) {
            return "Error during HTTP request: " + e.getMessage();
        }
    }*/
}
