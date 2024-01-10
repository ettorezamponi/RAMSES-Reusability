package tools.ezamponi.config;

import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.PropertiesInstanceConfig;
import tools.ezamponi.util.InetUtils;
import tools.ezamponi.util.InetUtilsProperties;

import java.util.Properties;

public class WebAppInstanceConfig extends MyDataCenterInstanceConfig {

    private static final String HOST_NAME = "eureka.hostname";
    private Properties properties;


    public WebAppInstanceConfig(Properties properties) {
        this.properties = properties;
    }


    @Override
    public String getHostName(boolean refresh) {
        return properties.getProperty(HOST_NAME, "localhost");
    }

    @Override
    public String getInstanceId() {
        InetUtilsProperties target = new InetUtilsProperties();
        InetUtils utils = new InetUtils(target);
        InetUtils.HostInfo hostInfo = utils.findFirstNonLoopbackHostInfo();
        return hostInfo.getHostname() + ":" + getVirtualHostName() + ":" + getNonSecurePort();
    }


}
