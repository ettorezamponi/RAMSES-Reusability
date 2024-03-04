package ramses.knowledge;

import ramses.knowledge.domain.architecture.Instance;
import ramses.knowledge.domain.architecture.InstanceStatus;
import ramses.knowledge.domain.architecture.ServiceConfiguration;
import ramses.knowledge.domain.persistence.ConfigurationRepository;
import ramses.knowledge.externalinterfaces.ProbeClient;
import ramses.knowledge.externalinterfaces.ServiceInfo;
import ramses.knowledge.parser.QoSParser;
import ramses.knowledge.parser.SystemArchitectureParser;
import ramses.knowledge.parser.SystemBenchmarkParser;
import ramses.knowledge.domain.KnowledgeService;
import ramses.knowledge.domain.adaptation.specifications.QoSSpecification;
import ramses.knowledge.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KnowledgeInit implements InitializingBean {
    @Autowired
    private KnowledgeService knowledgeService;
    @Autowired
    private ConfigurationRepository configurationRepository;
    @Autowired
    private ProbeClient probeClient;
    @Autowired
    private Environment environment;


    @Override
    public void afterPropertiesSet() throws Exception {
        String configDirPath = environment.getProperty("CONFIGURATION_PATH");
        if (configDirPath == null) {
            configDirPath = Paths.get("").toAbsolutePath().toString();
            log.warn("No configuration path specified. Using current working directory: {}", configDirPath);
        }
        // parse from json what SHOULD BE in the architecture runtime
        FileReader architectureReader = new FileReader(ResourceUtils.getFile(configDirPath+"/system_architecture.json"));
        List<Service> serviceList = SystemArchitectureParser.parse(architectureReader);
        //log.info("SERVICE LIST: "+serviceList);
        FileReader qoSReader = new FileReader(ResourceUtils.getFile(configDirPath+"/qos_specification.json"));
        Map<String, List<QoSSpecification>> servicesQoS = QoSParser.parse(qoSReader);
        FileReader benchmarkReader = new FileReader(ResourceUtils.getFile(configDirPath+"/system_benchmarks.json"));
        Map<String, List<SystemBenchmarkParser.ServiceImplementationBenchmarks>> servicesBenchmarks = SystemBenchmarkParser.parse(benchmarkReader);

        // It retrieves for each service registered in Eureka: serviceId, currentImplementationId, instances
        Map<String, ServiceInfo> probeSystemRuntimeArchitecture = probeClient.getSystemArchitecture();
        log.info("PROBE SYSTEM ARCHITECTURE:" + probeSystemRuntimeArchitecture.toString());

        serviceList.forEach(service -> {
            ServiceInfo serviceInfo = probeSystemRuntimeArchitecture.get(service.getServiceId());
            //log.info("SERVICE= " + service);
            log.info("ACTUAL SERVICE ID= " + service.getServiceId());
            // ServiceInfo(serviceId=RESTAURANT-SERVICE, currentImplementationId=restaurant-service, instances=[restaurant-service@sefa-restaurant-service:58085])
            if (serviceInfo == null)
                throw new RuntimeException("Service " + service.getServiceId() + " not found in the system  runtime architecture");
            List<String> instances = serviceInfo.getInstances();
            if (instances == null || instances.isEmpty()){
                throw new RuntimeException("No instances found for service " + service.getServiceId());
            }

            service.setCurrentImplementationId(serviceInfo.getCurrentImplementationId());
            service.setAllQoS(servicesQoS.get(service.getServiceId()));
            servicesBenchmarks.get(service.getServiceId()).forEach(serviceImplementationBenchmarks -> {
                serviceImplementationBenchmarks.getQoSBenchmarks().forEach((adaptationClass, value) ->
                        service.getPossibleImplementations()
                                .get(serviceImplementationBenchmarks.getServiceImplementationId())
                                .setBenchmark(adaptationClass, value));
            });

            instances.forEach(instanceId -> {
                if (!instanceId.split("@")[0].equals(service.getCurrentImplementationId()))
                    throw new RuntimeException("Service " + service.getServiceId() + " has more than one running implementation");
                service.createInstance(instanceId.split("@")[1]).setCurrentStatus(InstanceStatus.ACTIVE);
            });

            //check on GitHub repo the configuration
            service.setConfiguration(probeClient.getServiceConfiguration(service.getServiceId(), service.getCurrentImplementationId()));
            configurationRepository.save(service.getConfiguration());
            knowledgeService.addService(service);
            log.debug("SERVICE ADDED TO THE KNOWLEDGE: " + service);
        });

        for (Service service : serviceList) {
            ServiceConfiguration configuration = service.getConfiguration();
            if (configuration.getLoadBalancerType() != null && configuration.getLoadBalancerType().equals(ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM)) {
                if (configuration.getLoadBalancerWeights() == null) {
                     for (Instance instance : service.getInstances())
                        configuration.addLoadBalancerWeight(instance.getInstanceId(), 1.0/service.getInstances().size());
                } else if (!configuration.getLoadBalancerWeights().keySet().equals(service.getCurrentImplementation().getInstances().keySet())) {
                    throw new RuntimeException("Service " + service.getServiceId() + " has a load balancer weights map with different keys than the current implementation instances");
                }
            }
        }

        for (Service service : serviceList) {
            log.debug(service.toString());
        }
        log.info("Knowledge initialized");
    }
}
