package ramses.dashboard.externalinterfaces;

import ramses.knowledge.domain.Modules;
import ramses.knowledge.domain.adaptation.options.AdaptationOption;
import ramses.knowledge.domain.architecture.Instance;
import ramses.knowledge.domain.architecture.Service;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {
    @GetMapping("/rest/servicesMap")
    Map<String, Service> getServicesMap();

    @GetMapping("/rest/service/{serviceId}")
    Service getService(@PathVariable String serviceId);

    @GetMapping("/rest/service/{serviceId}/instance/{instanceId}")
    Instance getInstance(@PathVariable String serviceId, @PathVariable String instanceId);


    // Adaptation status functions
    @GetMapping("/rest/activeModule")
    Modules getActiveModule();

    @GetMapping("/rest/chosenAdaptationOptionsHistory")
    Map<String, List<AdaptationOption>> getChosenAdaptationOptionsHistory(@RequestParam int n);

    @GetMapping("/rest/failedModule")
    Modules getFailedModule();

}