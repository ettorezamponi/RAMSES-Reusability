package tools.ezamponi.config;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.appinfo.UniqueIdentifier;

import java.util.Map;

public class MyEurekaInstanceConfig implements EurekaInstanceConfig {
    private String instanceId;

    // Altri campi e metodi necessari per la tua configurazione

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String getAppname() {
        return null;
    }

    @Override
    public String getAppGroupName() {
        return null;
    }

    @Override
    public boolean isInstanceEnabledOnit() {
        return false;
    }

    @Override
    public int getNonSecurePort() {
        return 0;
    }

    @Override
    public int getSecurePort() {
        return 0;
    }

    @Override
    public boolean isNonSecurePortEnabled() {
        return false;
    }

    @Override
    public boolean getSecurePortEnabled() {
        return false;
    }

    @Override
    public int getLeaseRenewalIntervalInSeconds() {
        return 0;
    }

    @Override
    public int getLeaseExpirationDurationInSeconds() {
        return 0;
    }

    @Override
    public String getVirtualHostName() {
        return null;
    }

    @Override
    public String getSecureVirtualHostName() {
        return null;
    }

    @Override
    public String getASGName() {
        return null;
    }

    @Override
    public String getHostName(boolean b) {
        return null;
    }

    @Override
    public Map<String, String> getMetadataMap() {
        return null;
    }

    @Override
    public DataCenterInfo getDataCenterInfo() {
        return null;
    }

    @Override
    public String getIpAddress() {
        return null;
    }

    @Override
    public String getStatusPageUrlPath() {
        return null;
    }

    @Override
    public String getStatusPageUrl() {
        return null;
    }

    @Override
    public String getHomePageUrlPath() {
        return null;
    }

    @Override
    public String getHomePageUrl() {
        return null;
    }

    @Override
    public String getHealthCheckUrlPath() {
        return null;
    }

    @Override
    public String getHealthCheckUrl() {
        return null;
    }

    @Override
    public String getSecureHealthCheckUrl() {
        return null;
    }

    @Override
    public String[] getDefaultAddressResolutionOrder() {
        return new String[0];
    }

    @Override
    public String getNamespace() {
        return null;
    }

}

