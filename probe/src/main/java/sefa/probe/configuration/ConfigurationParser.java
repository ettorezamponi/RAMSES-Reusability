package sefa.probe.configuration;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import ramses.configparser.CustomProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;

@Slf4j
@Component
public class ConfigurationParser {
    @Autowired
    private EurekaClient discoveryClient;


    public ServiceConfiguration parsePropertiesAndCreateConfiguration(String serviceId, String serviceImplementationId) {
        //InstanceInfo configInstance = getConfigServerInstance();
        //String url = configInstance.getHomePageUrl() + "config-server/default/main/" + serviceId.toLowerCase() + ".properties";
        //Fetching configuration from http://sefa-configserver:58888/config-server/default/main/restaurant-service.properties

        String url = "http://maven-configserver:8081/configserver/" + serviceId.toLowerCase() + ".properties";
        log.debug("Fetching configuration from " + url);
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration(serviceId);
        ResponseEntity<String> response = new RestTemplate().getForEntity(url, String.class);
        // # Each custom property MUST be in the form: # PROPERTY_PARENT.SERVICE_ID.SERVICE_APP_NAME.[INSTANCE_ID | global].PROPERTY_ELEMENT[.PROPERTY_ELEMENT]* = PROPERTY_VALUE
        String[] lines = Arrays.stream(response.getBody().split("\n")).filter(line -> line.matches("([\\w\\.-])+=.+")).toArray(String[]::new);
        for (String line : lines) {
            try {
                String[] keyValue = line.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.startsWith("resilience4j.circuitbreaker.") && !key.endsWith("ignoreExceptions")) {
                    String[] parts = keyValue[0].split("\\.");
                    String cbName = parts[parts.length-2];
                    String propName = parts[parts.length-1];
                    serviceConfiguration.addCircuitBreakerProperty(cbName, propName, value);
                }
            } catch (Exception e) {
                log.error("Error parsing line {}", line);
                log.error(e.getMessage());
            }
        }
        serviceConfiguration.setTimestamp(new Date());
        return parseGlobalProperties(serviceConfiguration, serviceId, serviceImplementationId);
    }

    public ServiceConfiguration parseGlobalProperties(ServiceConfiguration configuration, String serviceId, String serviceImplementationId) {
        //InstanceInfo configInstance = getConfigServerInstance();
        //String url = configInstance.getHomePageUrl() + "config-server/default/main/application.properties";
        //  http://sefa-configserver:58888/config-server/default/main/application.properties

        String url = "http://maven-configserver:8081/configserver/" + serviceId.toLowerCase() + ".properties";
        log.debug("Fetching configuration from " + url);
        ResponseEntity<String> response = new RestTemplate().getForEntity(url, String.class);
        String[] lines = Arrays.stream(response.getBody().split("\n")).filter(line -> line.matches("([\\w\\.-])+=.+")).toArray(String[]::new);
        for (String line : lines) {
            try {
                String[] keyValue = line.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.startsWith("loadbalancing.")) {
                    CustomProperty customProperty = new CustomProperty(key, value);
                    if (customProperty.getPropertyElements().length == 1) {
                        String propertyName = customProperty.getPropertyElements()[0];
                        if (customProperty.isServiceGlobal() && propertyName.equals("type")) {
                            configuration.setLoadBalancerType(customProperty.getValue().equalsIgnoreCase("weighted_random") ? ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM : ServiceConfiguration.LoadBalancerType.UNKNOWN);
                        } else if (serviceId.equals(customProperty.getServiceId())) {
                            switch (propertyName) {
                                case "type" ->
                                        configuration.setLoadBalancerType(customProperty.getValue().equalsIgnoreCase("weighted_random") ? ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM : ServiceConfiguration.LoadBalancerType.UNKNOWN);
                                case "weight" ->
                                        configuration.addLoadBalancerWeight(serviceImplementationId + "@" + customProperty.getAddress(), Double.valueOf(customProperty.getValue()));
                            }
                        }
                    }
                }
                switch (key) {
                    // PARSE OTHER GLOBAL PROPERTIES HERE
                }
            } catch (Exception e) {
                log.error("Error parsing line {}, cause: {}", line, e.getMessage());
            }
        }
        return configuration;
    }



    private InstanceInfo getConfigServerInstance() {
        return discoveryClient.getApplication("CONFIG-SERVER").getInstances().get(0);
        // InstanceInfo [instanceId = config-server, appName = CONFIG-SERVER, hostName = sefa-configserver, status = UP, ipAddr = 172.21.0.4, port = 58888,
        // securePort = 443, dataCenterInfo = com.netflix.appinfo.MyDataCenterInfo@22ab4431
    }

}
