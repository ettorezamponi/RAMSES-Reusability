package sefa.probe.prometheus;

import com.netflix.appinfo.InstanceInfo;
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

@Slf4j
@Controller
public class PrometheusParser {
    @Value("${ACTUATOR_RELATIVE_PATH}")
    private String actuatorRelativePath;

    public InstanceMetricsSnapshot parse(InstanceInfo instanceInfo) {
        InstanceMetricsSnapshot instanceMetricsSnapshot = new InstanceMetricsSnapshot(instanceInfo.getAppName(), instanceInfo.getInstanceId());
        List<MetricFamily> metricFamilies;
        try {
            URL url = new URL(instanceInfo.getHealthCheckUrl());
            log.info("INSTANCE INFO:" + instanceInfo);
            // [instanceId = payment-proxy-1-service@sefa-payment-proxy-1-service:58090, appName = PAYMENT-PROXY-SERVICE,
            // hostName = sefa-payment-proxy-1-service, status = UP, ipAddr = 172.22.0.7, port = 58090, securePort = 443, dataCenterInfo = com.netflix.appinfo.MyDataCenterInfo@7d942
            //url = new URL(url, actuatorRelativePath+"/prometheus");
            log.debug("URL TO GIVE TO PROMETHEUS:" + url);
            PrometheusScraper scraper = new PrometheusScraper(url);
            log.debug("STEP OVVVEEE");
            metricFamilies = scraper.scrape();
            log.debug("METRICS FAMILIES: " + metricFamilies.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, HttpEndpointMetrics> httpMetricsMap = new HashMap<>();

        metricFamilies.forEach(metricFamily -> {
            String propertyName = metricFamily.getName(); //e.g. http_server_requests_seconds
            log.info("PROPERTY NAME: " + propertyName);
            //MetricType metricType = elem.getType(); //e.g. GAUGE
            metricFamily.getMetrics().forEach(metric -> { //e.g., one metric is the http_server_requests_seconds for the endpoint X
                //log.info("METRIC: " + metric); --> "METRIC: prometheus.types.Gauge@8663d4a"
                Map<String, String> labels = metric.getLabels();
                switch (propertyName) {
                    case PrometheusMetrics.HTTP_REQUESTS_TIME ->
                            handleHttpServerRequestsTotalDurationMs(httpMetricsMap, (Histogram) metric);
                    case PrometheusMetrics.HTTP_REQUESTS_MAX_TIME ->
                            handleHttpServerRequestsMaxDuration(httpMetricsMap, (Gauge) metric);
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
                        log.warn("PropertyName not founded for the metric: ", metric.getLabels());
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

    private void handleHttpServerRequestsTotalDurationMs(Map<String, HttpEndpointMetrics> httpMetricsMap, Histogram metric) {
        Map<String, String> labels = metric.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
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
