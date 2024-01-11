package tools.ezamponi.config;

import com.netflix.appinfo.MyDataCenterInstanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.ezamponi.MyEurekaClientService;

import java.util.Properties;

public class WebAppInstanceConfig extends MyDataCenterInstanceConfig {

    private static final String HOST_NAME = "eureka.hostname";
    private Properties properties;
    private Logger logger = LoggerFactory.getLogger(MyEurekaClientService.class);


    public WebAppInstanceConfig(Properties properties) {
        this.properties = properties;
    }


    @Override
    public String getHostName(boolean refresh) {
        return properties.getProperty(HOST_NAME, "localhost");
    }

    // this prevented the id from being set correctly via properties
    /*@Override
    public String getInstanceId() {
        InetUtilsProperties target = new InetUtilsProperties();
        InetUtils utils = new InetUtils(target);
        InetUtils.HostInfo hostInfo = utils.findFirstNonLoopbackHostInfo();
        return hostInfo.getHostname() + ":" + getVirtualHostName() + ":" + getNonSecurePort();
    }*/


}
