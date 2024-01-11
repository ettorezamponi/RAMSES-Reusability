package tools.ezamponi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyEurekaClientHelper {

    static MyEurekaClientService myEurekaClientService = new MyEurekaClientService();
    private static final Logger LOG = LoggerFactory.getLogger(MyEurekaClientHelper.class);

    public static void register() {

        try {
            LOG.info("Registration started after 5 seconds");
            myEurekaClientService.registerInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deRegister() {
        //TODO check if it works
        myEurekaClientService.deRegister();
    }

}
