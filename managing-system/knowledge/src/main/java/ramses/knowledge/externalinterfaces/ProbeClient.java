package ramses.knowledge.externalinterfaces;

import ramses.knowledge.domain.architecture.ServiceConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "PROBE", url = "${PROBE_URL}")
public interface ProbeClient {
    @GetMapping("/rest/systemArchitecture")
    Map<String, ServiceInfo> getSystemArchitecture();

    @GetMapping("/rest/service/{serviceId}/configuration")
    ServiceConfiguration getServiceConfiguration(@PathVariable("serviceId") String serviceId, @RequestParam("implementationId") String implementationId);

}