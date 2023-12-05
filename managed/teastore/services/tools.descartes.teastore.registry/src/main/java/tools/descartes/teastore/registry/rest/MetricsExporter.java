package tools.descartes.teastore.registry.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/metrics")
public class MetricsExporter {
    private static final Logger LOG = LoggerFactory.getLogger(RegistryStartup.class);
    MeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);


    @GET
    public String getMetrics() {
        exporter(registry);
        return convertMetricsToJson(registry);
    }

    private void exporter(MeterRegistry meterRegistry) {

        new ClassLoaderMetrics().bindTo(meterRegistry);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);

        LOG.info("*** PROMETHEUS METRICS ***");

        //meterRegistry.close();
    }

    private static String convertMetricsToJson(MeterRegistry meterRegistry) {
        List<Map<String, Object>> metricsList = new ArrayList<>();

        for (Meter meter : meterRegistry.getMeters()) {
            Map<String, Object> metricInfo = new HashMap<>();
            metricInfo.put("name", meter.getId().getName());
            //metricInfo.put("type", meter.getId().getType());
            //metricInfo.put("tags", convertTagsToJson(meter.getId().getTags()));
            metricInfo.put("measurements", meter.measure());
            metricsList.add(metricInfo);
        }

        try {
            return new ObjectMapper().writeValueAsString(metricsList);
        } catch (IOException e) {
            e.printStackTrace();
            return "{Error while writing values as string}";
        }
    }

    private static Map<String, String> convertTagsToJson(Iterable<Tag> tags) {
        Map<String, String> tagMap = new HashMap<>();
        for (Tag tag : tags) {
            tagMap.put(tag.getKey(), tag.getValue());
        }
        return tagMap;
    }
}
