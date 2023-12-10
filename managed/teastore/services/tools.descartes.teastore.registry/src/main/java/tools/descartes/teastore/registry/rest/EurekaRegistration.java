package tools.descartes.teastore.registry.rest;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.PollResult;
import com.netflix.config.sources.URLConfigurationSource;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.jersey.JerseyApplicationClient;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.shenyu.common.utils.IpUtils;
import org.apache.shenyu.registry.api.ShenyuInstanceRegisterRepository;
import org.apache.shenyu.registry.api.config.RegisterConfig;
import org.apache.shenyu.registry.api.entity.InstanceEntity;
import org.apache.shenyu.spi.Join;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class EurekaRegistration implements ShenyuInstanceRegisterRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(EurekaRegistration.class);

    private EurekaClient eurekaClient;

    private EurekaHttpClient eurekaHttpClient;

    public void setServerProperties() {
        ConfigurationManager.getConfigInstance().setProperty("eureka.client.service-url.defaultZone", "http://sefa-eureka:58082/eureka/");
        ConfigurationManager.getConfigInstance().setProperty("eureka.serviceUrl.default", "http://sefa-eureka:58082/eureka/"); //config.getServerLists()
        ConfigurationManager.getConfigInstance().setProperty("eureka.client.registerWithEureka", "true");
        ConfigurationManager.getConfigInstance().setProperty("eureka.client.appname", "Teastore-Registry");
        ConfigurationManager.getConfigInstance().setProperty("eureka.client.home-page-url", "http://localhost:8080/");
        ConfigurationManager.getConfigInstance().setProperty("eureka.client.fetchRegistry", "true");
        ConfigurationManager.getConfigInstance().setProperty("eureka.instance.hostname", "localhost");
        ConfigurationManager.getConfigInstance().setProperty("eureka.instance.instance-id", "teastore-registry@teastore-registry:8080");
        ConfigurationManager.getConfigInstance().setProperty("eureka.instance.lease-renewal-interval-in-seconds", "2");
        //ConfigurationManager.getConfigInstance().setProperty("eureka.instance.lease-expiration-duration-in-seconds", "1");
    }

    @Override
    public void init(final RegisterConfig config) {
        setServerProperties();
        LOGGER.info("****************** configuration setted");
        ApplicationInfoManager applicationInfoManager = initializeApplicationInfoManager(new MyDataCenterInstanceConfig());
        LOGGER.info("****************** application info manager setted");
        eurekaClient = new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig());
        eurekaHttpClient = new JerseyApplicationClient(new ApacheHttpClient4(), config.getServerLists(), null);
        LOGGER.info("****************** eureka client setted");
    }

    public ApplicationInfoManager initializeApplicationInfoManager(final EurekaInstanceConfig instanceConfig) {
        InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
        // [instanceId = edb6bf505add, appName = UNKNOWN, hostName = edb6bf505add, status = STARTING, ipAddr = 172.18.0.4,
        // port = 80, securePort = 443, dataCenterInfo = com.netflix.appinfo.PropertiesInstanceConfig$1@5c805eba
        return new ApplicationInfoManager(instanceConfig, instanceInfo);
    }

    @Override
    public void persistInstance(final InstanceEntity instance) {
        EurekaHttpResponse<Void> register = eurekaHttpClient.register(generateInstanceInfo(instance));
        LOGGER.info("eureka client register success: {}", register.getEntity());
    }

    private InstanceInfo generateInstanceInfo(final InstanceEntity instance) {
        return InstanceInfo.Builder.newBuilder()
                .setAppName(instance.getAppName())
                .setIPAddr(IpUtils.getHost())
                .setHostName(instance.getHost())
                .setPort(instance.getPort())
                .setDataCenterInfo(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn))
                .build();
    }

    @Override
    public List<InstanceEntity> selectInstances(final String selectKey) {
        return getInstances(selectKey);
    }

    private List<InstanceEntity> getInstances(final String selectKey) {
        List<InstanceInfo> instances = eurekaClient.getInstancesByVipAddressAndAppName(null, selectKey, true);
        return instances.stream()
                .map(i -> InstanceEntity.builder()
                        .appName(i.getAppName()).host(i.getHostName()).port(i.getPort())
                        .build()
                ).collect(Collectors.toList());
    }

    @Override
    public void close() {
        eurekaClient.shutdown();
        LOGGER.debug("EUREKA CLOSE METHOD!!");
    }

}
