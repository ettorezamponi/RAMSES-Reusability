package sefa.probe.prometheus;

import com.netflix.appinfo.InstanceInfo;
import io.micrometer.core.instrument.Timer;
import sefa.probe.domain.metrics.HttpEndpointMetrics;
import sefa.probe.domain.metrics.InstanceMetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import prometheus.PrometheusScraper;
import prometheus.types.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Controller
public class PrometheusParser {
    @Value("${ACTUATOR_RELATIVE_PATH}")
    private String actuatorRelativePath;
    String outcome;
    String status;

    public InstanceMetricsSnapshot parse(InstanceInfo instanceInfo) {
        InstanceMetricsSnapshot instanceMetricsSnapshot = new InstanceMetricsSnapshot(instanceInfo.getAppName(), instanceInfo.getInstanceId());
        List<MetricFamily> metricFamilies;
        try {
            URL url = new URL(instanceInfo.getHealthCheckUrl());
            // log.info("INSTANCE INFO:" + instanceInfo);
            // [instanceId = payment-proxy-1-service@sefa-payment-proxy-1-service:58090, appName = PAYMENT-PROXY-SERVICE,
            // hostName = sefa-payment-proxy-1-service, status = UP, ipAddr = 172.22.0.7, port = 58090, securePort = 443, dataCenterInfo = com.netflix.appinfo.MyDataCenterInfo@7d942
            //url = new URL(url, actuatorRelativePath+"/prometheus");
            log.debug("URL TO GIVE TO PROMETHEUS:" + url);
            PrometheusScraper scraper = new PrometheusScraper(url);
            metricFamilies = scraper.scrape();

            /*metricFamilies.forEach(metricFamily -> {
                String metricName = metricFamily.getName();
                System.out.println("Metric Family Name: " + metricName);
            });*/
            //Metric Family Name: jvm_buffer_count_buffers
            //Metric Family Name: jvm_threads_peak_threads
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, HttpEndpointMetrics> httpMetricsMap = new HashMap<>();

        metricFamilies.forEach(metricFamily -> {
            String propertyName = metricFamily.getName(); // e.g. http_server_requests_seconds
            //log.info("PROPERTY NAME: " + propertyName); // e.g. jvm_classes_unloaded_classes_total
            //TODO vedi se utilizzare questa funzione random adeguatamente
            //generateOutcomeStatus();

            //MetricType metricType = elem.getType(); e.g. GAUGE
            metricFamily.getMetrics().forEach(metric -> { //e.g., one metric is the http_server_requests_seconds for the endpoint X
                //log.info("METRIC: " + metric); e.g. METRIC: prometheus.types.Gauge@8663d4a
                Map<String, String> labels = metric.getLabels();
                //log.info("METRIC LABEL: " + metric.getLabels() ); // e.g., {area=nonheap, id=CodeHeap 'profiled nmethods'},nullnull OPPURE {},nullnull
                switch (propertyName) {
                    case JavamelodyMetrics.HTTP_REQUESTS_TIME ->
                            handleHttpServerRequestsTotalDurationMs(httpMetricsMap, (Histogram) metric);
                    case JavamelodyMetrics.HTTP_REQUESTS_MAX_TIME ->
                            handleHttpServerRequestsMaxDurationForJM(httpMetricsMap, (Gauge) metric);
                    case PrometheusMetrics.DISK_FREE_SPACE ->
                            instanceMetricsSnapshot.setDiskFreeSpace(((Gauge) metric).getValue());
                    case PrometheusMetrics.DISK_TOTAL_SPACE ->
                            instanceMetricsSnapshot.setDiskTotalSpace(((Gauge) metric).getValue());
                    case PrometheusMetrics.CPU_USAGE ->
                            instanceMetricsSnapshot.setCpuUsage(((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_BUFFERED_CALLS ->
                            instanceMetricsSnapshot.addCircuitBreakerBufferedCalls(labels.get("name"), labels.get("kind"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_STATE ->
                            instanceMetricsSnapshot.addCircuitBreakerState(labels.get("name"), labels.get("state"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_CALLS_SECONDS ->
                            instanceMetricsSnapshot.addCircuitBreakerCallCountAndDurationSum(labels.get("name"), labels.get("kind"), (int) ((Summary) metric).getSampleCount(), ((Summary) metric).getSampleSum());
                    case PrometheusMetrics.CB_CALLS_SECONDS_MAX ->
                            instanceMetricsSnapshot.addCircuitBreakerCallMaxDuration(labels.get("name"), labels.get("kind"), ((Gauge)metric).getValue());
                    case PrometheusMetrics.CB_NOT_PERMITTED_CALLS_TOTAL ->
                            instanceMetricsSnapshot.addCircuitBreakerNotPermittedCallsCount(labels.get("name"), (int) ((Counter) metric).getValue());
                    case PrometheusMetrics.CB_SLOW_CALL_RATE ->
                            instanceMetricsSnapshot.addCircuitBreakerSlowCallRate(labels.get("name"), ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_SLOW_CALLS ->
                            instanceMetricsSnapshot.addCircuitBreakerSlowCallCount(labels.get("name"), labels.get("kind"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_FAILURE_RATE ->
                            instanceMetricsSnapshot.addCircuitBreakerFailureRate(labels.get("name"), ((Gauge) metric).getValue());
                    default -> {
                        //log.warn("PropertyName not founded for the metric: ", metric.getLabels());
                    }
                }
            });
        } );
        instanceMetricsSnapshot.setHttpMetrics(httpMetricsMap);
        return instanceMetricsSnapshot;
    }

    private boolean isAnExcludedUrl(String url) {
        return url.contains("/actuator/");
    }

    private void generateOutcomeStatus() {
        if (new Random().nextInt(4) == 0) { // numero casuale da 0 a 3, 25% che sia 0
            outcome = "SERVER_ERROR";
            status = "500";
            //log.info("ERROR GENERATED FOR HTTP METRICS");
        } else {
            outcome = "SUCCESS";
            status = "200";
            //log.info("SUCCESS GENERATED FOR HTTP METRICS");
        }
    }

    // Method modified to make a fake Histogram metric from the estimated value from javaMelody
    private void handleHttpServerRequestsTotalDurationForJM(Map<String, HttpEndpointMetrics> httpMetricsMap, Gauge metric) {
        Histogram histogram = new Histogram.Builder()
                .setName(metric.getName())
                .addLabel("exception", "None")
                .addLabel("method", "GET")
                .addLabel("outcome", "SUCCESS")
                .addLabel("status", "200")
                .addLabel("uri", "/generic")
                .setSampleCount(Math.round(metric.getValue()))
                .build();

        //log.info("HANDLE HTTP REQUEST TOTAL DURATION LAUNCHED");
        Map<String, String> labels = histogram.getLabels();
        //log.info("HTTP LABELS: " + labels);

        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsDetails(labels.get("outcome"), Integer.parseInt(labels.get("status")), (int) histogram.getSampleCount(), histogram.getSampleCount() * 1000);
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);

        //log.info("HTTP METRICS CREATED: "+metrics);
    }

    private String getRandomUri() {
        String[] uris = {"/cartAction", "/loginAction", "/product", "/category"};
        Random random = new Random();
        int randomIndex = random.nextInt(uris.length);
        return uris[randomIndex];
    }

    // Method modified to make a fake Histogram metric from the estimated value from javaMelody
    private void handleHttpServerRequestsMaxDurationForJM(Map<String, HttpEndpointMetrics> httpMetricsMap, Gauge metric) {
        Gauge gauge = new Gauge.Builder()
                .setName(metric.getName())
                .addLabel("exception", "None")
                .addLabel("method", "GET")
                .addLabel("outcome", "SUCCESS")
                .addLabel("status", "200")
                .addLabel("uri", "/generic")
                .setValue(metric.getValue())
                .build();

        //log.info("HANDLE HTTP REQUEST MAX DURATION LAUNCHED");
        Map<String, String> labels = gauge.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]

        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsMaxDuration(labels.get("outcome"), gauge.getValue()); //yet in milliseconds
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);

        //log.info("MAX HTTP METRICS CREATED: "+metrics);
    }

    private void handleHttpServerRequestsTimer(Map<String, HttpEndpointMetrics> httpMetricsMap, Timer metric) {
        System.out.print("TAG IN TIMER: "+metric.getId().getTags());

/*
        Map<String, String> labels = metric.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        if (isAnExcludedUrl(labels.get("uri")))
            return;
        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsDetails(labels.get("outcome"), Integer.parseInt(labels.get("status")), (int) metric.getSampleCount(), metric.getSampleSum()*1000);
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);
*/
    }

    private void handleHttpServerRequestsTotalDurationMs(Map<String, HttpEndpointMetrics> httpMetricsMap, Histogram metric) {
        Map<String, String> labels = metric.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        System.out.println("TIMER LABELS: "+labels);
        if (isAnExcludedUrl(labels.get("uri")))
            return;
        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsDetails(labels.get("outcome"), Integer.parseInt(labels.get("status")), (int) metric.getSampleCount(), metric.getSampleSum()*1000);
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);
    }

    private void handleHttpServerRequestsMaxDuration(Map<String, HttpEndpointMetrics> httpMetricsMap, Gauge metric) {
        Map<String, String> labels = metric.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        if (isAnExcludedUrl(labels.get("uri")))
            return;
        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsMaxDuration(labels.get("outcome"), metric.getValue()*1000);
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);
    }

}
