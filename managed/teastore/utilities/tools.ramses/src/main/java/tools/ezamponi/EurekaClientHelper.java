package tools.ezamponi;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.smattme.eureka.client.wrapper.EurekaClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EurekaClientHelper {

    static EurekaClientService eurekaClientService = new EurekaClientService();
    private static final Logger LOG = LoggerFactory.getLogger(EurekaClientHelper.class);

    public static void register() {

        try {
            LOG.info("Registration started after 5 seconds");
            eurekaClientService.registerInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deRegister() {
        //TODO check if it works
        eurekaClientService.deRegister();
    }
}
