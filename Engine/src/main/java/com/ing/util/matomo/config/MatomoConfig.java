package com.ing.util.matomo.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration handler for Matomo tracking settings.
 * Reads configuration from matomo.properties file.
 */
public class MatomoConfig {
    private static final Logger logger = LoggerFactory.getLogger(MatomoConfig.class);
    private static final String CONFIG_FILE = "matomo.properties";

    private final String matomoUrl;
    private final int siteId;
    private Properties properties;

    /**
     * Constructs MatomoConfig by loading properties from matomo.properties file.
     *
     * @throws IllegalStateException if configuration file cannot be loaded or required properties are missing
     */
    public MatomoConfig(String location) {
        // logger.info("Matomo configuration location: location={}",
        //            location);
        this.properties = loadProperties(location);
        this.matomoUrl = getRequiredProperty(this.properties, "URL");
        this.siteId = Integer.parseInt(getRequiredProperty(this.properties, "siteID"));
        // logger.info("Matomo configuration loaded: URL={}, SiteId={}",
        //            matomoUrl, siteId);
    }

    /**
     * Constructs MatomoConfig with explicit values (for testing or programmatic configuration).
     *
     * @param matomoUrl the Matomo tracking API URL
     * @param siteId the Matomo site ID
     */
    public MatomoConfig(String matomoUrl, int siteId) {
        this.matomoUrl = matomoUrl;
        this.siteId = siteId;
    }

    // /**
    //  * Loads properties from the configuration file.
    //  *
    //  * @return Properties object containing configuration
    //  * @throws IllegalStateException if file cannot be loaded
    //  */
    // private Properties loadProperties() {
    //     Properties properties = new Properties();
    //     try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
    //         if (input == null) {
    //             throw new IllegalStateException(
    //                 "Unable to find " + CONFIG_FILE + " in classpath. " +
    //                 "Please create this file based on matomo.properties.example"
    //             );
    //         }
    //         properties.load(input);
    //         return properties;
    //     } catch (IOException e) {
    //         throw new IllegalStateException("Error loading " + CONFIG_FILE, e);
    //     }
    // }

    private static Properties loadProperties(String location) {
        Properties prop = new Properties();
        if (new File(location).exists()) {
            try (FileInputStream inputStream = new FileInputStream(location)) {
                prop.load(inputStream);
            } catch (IOException ex) {
                logger.error("Error loading properties from " + location, ex);
            }
        }
        return prop;
    }

    /**
     * Gets a required property from Properties object.
     *
     * @param properties the Properties object
     * @param key the property key
     * @return the property value
     * @throws IllegalStateException if property is missing
     */
    private String getRequiredProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                "Required property '" + key + "' is missing in " + CONFIG_FILE
            );
        }
        return value.trim();
    }

    public String getProperty(String key) {
        return this.properties.getProperty(key);
    }

    public String getMatomoUrl() {
        return matomoUrl;
    }

    public int getSiteId() {
        return siteId;
    }

    @Override
    public String toString() {
        return "MatomoConfig{" + "matomoUrl='" + matomoUrl + '\'' + ", siteId=" + siteId + '}';
    }
}
