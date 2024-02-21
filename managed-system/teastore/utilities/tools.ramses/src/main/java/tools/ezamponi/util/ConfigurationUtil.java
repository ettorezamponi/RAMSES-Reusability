package tools.ezamponi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class ConfigurationUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);

    public static Properties loadCascadedProperties(String configName) {

        Properties props = new Properties();
        String defaultConfigFileName = configName + ".properties";
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(defaultConfigFileName);
        if (url == null) {
            logger.error("Cannot locate " + defaultConfigFileName + " as a classpath resource.");
            return props;
        }

        try {
            props.load(url.openStream());
        } catch (IOException e) {
            logger.error("Error while loading properties from " + defaultConfigFileName + ": " + e.getMessage(), e);
        }

        logger.info("final properties: \n" + props.values());

        return props;
    }

}
