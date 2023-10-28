package ramses.monitor.externalinterfaces;

import ramses.knowledge.domain.architecture.ServiceConfiguration;
import ramses.knowledge.domain.metrics.InstanceMetricsSnapshot;
import ramses.knowledge.externalinterfaces.ServiceInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "PROBE", url = "${PROBE_URL}")
public interface ProbeClient {
    @GetMapping("/rest/service/{serviceId}/snapshot")
    List<InstanceMetricsSnapshot> takeSnapshot(@PathVariable("serviceId") String serviceId);

    @GetMapping("/rest/systemArchitecture")
    Map<String, ServiceInfo> getSystemArchitecture();

    @GetMapping("/rest/service/{serviceId}/configuration")
    ServiceConfiguration getServiceConfiguration(@PathVariable("serviceId") String serviceId, @RequestParam("implementationId") String implementationId);

}