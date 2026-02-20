
package com.ing.datalib.settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.ing.datalib.util.data.LinkedProperties;


/**
 * Manages API configuration files stored in a designated directory.
 * <p>
 * This class provides functionality to create, load, modify, save, and delete
 * API configuration files represented as {@link Properties} objects. Each configuration
 * is identified by a unique alias and stored as a `.properties` file within the API folder.
 * <p>
 * Key features include:
 * <ul>
 *     <li>Automatic loading of existing configuration files</li>
 *     <li>Creation of default configuration files if none exist</li>
 *     <li>Support for adding new API aliases and their properties</li>
 *     <li>Persistent saving and deletion of configuration files</li>
 *     <li>Access to individual or all API configurations</li>
 * </ul>
 * <p>
 * The class relies on utility methods from {@code PropUtils} for reading and writing
 * properties files, and maintains internal mappings for efficient access.
 *
 * @author Renz Jephte Moreno
 */
public class DriverProperties extends LinkedProperties {
    
    private static String geckoDriverPath, chromeDriverPath;
    private static ArrayList<String> apiConfigList = new ArrayList<>();
    private final Map<String, Properties> apiConfigFilePropMap = new HashMap<>();
    private static String location;
    private String currLoadedAPIConfig;

    static {
        setDriverPath();
    }

    public DriverProperties(String location) {
        this.location = location;
        createAPIFolder();
        load();
        currLoadedAPIConfig = "default";
   }

    /**
     * Loads API configuration properties from files located at the specified location.
     * <p>
     * This method performs the following steps:
     * <ul>
     *     <li>Clears the current list of API configuration aliases.</li>
     *     <li>Checks if the configuration directory exists; if not, the method exits.</li>
     *     <li>Iterates over all `.properties` files in the directory.</li>
     *     <li>Extracts the alias from each file name.</li>
     *     <li>If the alias is not already in the list, adds it and loads the properties into a map.</li>
     * </ul>
     */
    private void load() {
        apiConfigList.clear();
        File apiFile = new File(getLocation());

        if (!apiFile.exists()) return;

        for (File file : getPropertiesFiles(apiFile)) {
            String alias = file.getName().replace(".properties", "");
            if (apiConfigList.add(alias)) {
                apiConfigFilePropMap.put(alias, PropUtils.load(file));
            }
        }
    }

    /**
     * Retrieves all `.properties` files from the specified directory.
     * <p>
     * This method filters the contents of the given directory and returns
     * an array of {@link File} objects representing files that end with the
     * ".properties" extension.
     *
     * @param directory the directory to search for `.properties` files
     * @return an array of {@code File} objects representing the matching files,
     *         or {@code null} if the directory does not exist or an I/O error occurs
     */
    private File[] getPropertiesFiles(File directory) {
        return directory.listFiles((dir, name) -> name.endsWith(".properties"));
    }
    
    
//    private void loadDefault() {
//        setSSLCertificateVerification(getSSLCertificateVerification());
//        setUseProxy(getUseProxy());
//        setProxyHost(getProxyHost());
//        setProxyPort(getProxyPort());
//        
//        setSelfSigned(getSelfSigned());
//        setKeyStorePath(getKeyStorePath());
//        setKeyStorePassword(getKeyStorePassword());
//        
//    }


    /**
     * Creates the API configuration folder if it does not exist, and initializes it with a default configuration file.
     */
    private void createAPIFolder() {
        File apiFolder = new File(getLocation());
        if (!apiFolder.exists()) {
            apiFolder.mkdirs();
            createDefaultFile(getLocation());
        }
    }

    
    /**
     * Creates a default API configuration file in the specified location.
     * <p>
     * This method attempts to copy an existing {@code DriverSettings.Properties} file
     * from the parent directory of the given location to a new file named {@code default.properties}.
     * If the source file exists, it is copied and updated with a default alias property.
     * If the source file does not exist, a new properties file is created with default API settings.
     * <p>
     * Any I/O errors encountered during this process are logged using the system logger.
     *
     * @param location the path where the default properties file should be created
     */
    private void createDefaultFile(String location) {
        System.out.println("creating properties : " + location);
        Path driverSettingProperties = Paths.get(location).getParent().resolve("DriverSettings.Properties");
        Path apiDefaultFile = Paths.get(location, "default.properties");
        
        try {
            if (Files.exists(driverSettingProperties)){
                Files.copy(driverSettingProperties, apiDefaultFile);
                Properties prop = PropUtils.load(new File(driverSettingProperties.toString()));
                prop.setProperty("api.alias", "default");
                PropUtils.save(prop, apiDefaultFile.toString());
            } else  {             
                Properties prop = setAPIProperties(new Properties());
                PropUtils.save(prop, apiDefaultFile.toString());
            } 
        } catch (IOException ex) {
                System.getLogger(DriverProperties.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    
    /**
     * Sets default API properties on the given {@link Properties} object.
     * <p>
     * This method assigns the default alias value and delegates additional
     * default property settings to {@link #setDefaultProperties(Properties)}.
     *
     * @param prop the {@code Properties} object to populate with default API settings
     * @return the updated {@code Properties} object with default values applied
     */
    private Properties setAPIProperties(Properties prop) {
        prop.setProperty("api.alias", "default");
        setDefaultProperties(prop);
        return prop;
    }

        
    /**
     * Sets API properties on the given {@link Properties} object using a specified alias.
     * <p>
     * This method assigns the provided alias to the {@code api.alias} property and
     * delegates additional default property settings to {@link #setDefaultProperties(Properties)}.
     *
     * @param prop the {@code Properties} object to populate with API settings
     * @param apiAlias the alias to assign to the {@code api.alias} property
     * @return the updated {@code Properties} object with the specified alias and default values
     *
     * @see #setDefaultProperties(Properties)
     */
    private Properties setAPIProperties(Properties prop, String apiAlias) {
        prop.setProperty("api.alias", apiAlias);
        setDefaultProperties(prop);
        return prop;
    }

    
    /**
     * Sets default properties on the given {@link Properties} properties.
     * <p>
     * This method initializes the following keys with default values:
     * <ul>
     *     <li>{@code proxyPort} – empty string</li>
     *     <li>{@code setSSLCertVerification} – {@code false}</li>
     *     <li>{@code useProxy} – {@code false}</li>
     *     <li>{@code proxyHost} – empty string</li>
     * </ul>
     *
     * @param prop the {@code Properties} object to populate with default values
     */
    private void setDefaultProperties(Properties prop) {
        prop.setProperty("proxyPort", "");
        prop.setProperty("setSSLCertVerification", "false");
        prop.setProperty("useProxy", "false");
        prop.setProperty("proxyHost", "");
        prop.setProperty("httpClientRedirect", "NEVER");
    }
    
    
    /**
     * Constructs the full file path for the specified API configuration file.
     * 
     * @param apiName the name of the API
     * @return the full path to the API's properties file
     * 
     */
    public String getAPILocation(String apiName) {
        return getLocation() + File.separator + apiName + ".properties";
    }

    
    /**
     * Returns the path to the API configuration directory.
     *
     * @return the full path to the API configuration directory
     */
    public static String getLocation() {
        return location + File.separator + "API";
    }
    
    
    /**
     * Sets the base location for storing API configuration files.
     *
     * @param location the base directory path
     */
    public void setLocation(String location) {
        this.location = location;
    }

    
    /**
     * Loads and returns the list of available API aliases.
     * <p>
     * This method ensures the list is refreshed by calling {@link #load()} before returning.
     *
     * @return a list of API aliases
     */
    public ArrayList<String> getAPIList() {
        load();
        return apiConfigList;
    }

    
    /**
     * Retrieves the properties associated with the specified API alias.
     *
     * @param apiName the alias of the API
     * @return the {@code Properties} object for the given API, or {@code null} if not found
     */
    public Properties getAPIPropertiesFor(String apiName) {
        return apiConfigFilePropMap.get(apiName);
    }

    
    /**
     * Adds a new API alias to the list of known APIs.
     *
     * @param apiName the alias to add
     */
    public void addAPIName(String apiName) {
        apiConfigList.add(apiName);
    }

    
    /**
     * Creates and adds a new API configuration using the specified alias.
     * <p>
     * A new {@code Properties} object is initialized with default values and the given alias,
     * then added to the configuration map.
     *
     * @param apiName the alias of the new API
     *
     * @see #setAPIProperties(Properties, String)
     * @see #addAPI(String, Properties)
     */
    public void addAPIProperty(String apiName) {
        Properties prop = new Properties();
        setAPIProperties(prop, apiName);
        addAPI(apiName, prop);
    }

    
    /**
     * Adds a new API configuration to the internal map and persists the changes.
     * <p>
     * The provided {@link Properties} object is associated with the given API alias
     * and stored in the configuration map. After updating the map, the method calls {@link #save()}
     * to persist the changes to disk.
     *
     * @param apiName the alias of the API
     * @param prop the {@code Properties} object containing the API configuration
     *
     * @see #save()
     */
    public void addAPI(String apiName, Properties prop) {
        apiConfigFilePropMap.put(apiName, prop);
        save();
    }
    
    
    /**
     * Saves all API configuration properties to their respective files.
     * <p>
     * This method iterates through the internal map of API aliases and their corresponding
     * {@link Properties} objects, and writes each one to disk using the file path
     * generated by {@link #getAPILocation(String)}.
     *
     * @see PropUtils#save(Properties, String)
     */
    public void save() {
        for (Map.Entry<String, Properties> entry : apiConfigFilePropMap.entrySet()) {
            String apiName = entry.getKey();
            Properties apiProp = entry.getValue();
            PropUtils.save(apiProp, getAPILocation(apiName));
        }
    }
    
    
    /**
     * Deletes the API configuration associated with the specified alias.
     * <p>
     * This method performs the following actions:
     * <ul>
     *     <li>Checks if the alias exists in the configuration map.</li>
     *     <li>If the corresponding properties file exists on disk, it is deleted.</li>
     *     <li>Removes the alias from both the configuration map and the list of known APIs.</li>
     * </ul>
     *
     * @param apiName the alias of the API to delete
     *
     * @see #getAPILocation(String)
     */
    public void delete(String apiName) {
        if (apiConfigFilePropMap.containsKey(apiName)) {
            File api = new File(getAPILocation(apiName));
            if (api.exists()) {
                api.delete();
            }
            apiConfigFilePropMap.remove(apiName);
            apiConfigList.remove(apiName);
        }
    }
    
    /**
     * Sets the currently loaded API configuration alias.
     * <p>
     * This value can be used to track which API configuration is actively in use.
     *
     * @param currLoadedAPIConfig the alias of the currently loaded API configuration
     */
    public void setCurrLoadedAPIConfig(String currLoadedAPIConfig) {
        this.currLoadedAPIConfig = currLoadedAPIConfig;
    }
    
    /**
     * Checks whether an API configuration with the specified alias exists.
     *
     * @param apiConfig the alias of the API configuration to check
     * @return {@code true} if the alias exists in the configuration list; {@code false} otherwise
     */
    public Boolean doesAPIconfigExist(String apiConfig) {
        return apiConfigList.contains(apiConfig);
    }

    // Driverpath related methods
    private static void setDriverPath() {
        if (System.getProperty("os.name", "")
                .toLowerCase().contains("win")) {
            geckoDriverPath = "./lib/Drivers/geckodriver.exe";
            chromeDriverPath = "./lib/Drivers/chromedriver.exe";
        } else {
            geckoDriverPath = "./lib/Drivers/geckodriver";
            chromeDriverPath = "./lib/Drivers/chromedriver";
        }
    }
    
    public void setFirefoxBinaryPath(String path) {
        setProperty("FirefoxBinaryPath", path);
    }

    public void setGeckcoDriverPath(String path) {
        setProperty("GeckoDriverPath", path);
    }

    public void setChromeDriverPath(String path) {
        setProperty("ChromeDriverPath", path);
    }

    public void setIEDriverPath(String path) {
        setProperty("IEDriverPath", path);
    }

    public void setEdgeDriverPath(String path) {
        setProperty("EdgeDriverPath", path);
    }

    public String getFirefoxBinaryPath() {
        return getProperty("FirefoxBinaryPath", "");
    }

    public String getGeckcoDriverPath() {
        return getProperty("GeckoDriverPath", geckoDriverPath);
    }

    public String getChromeDriverPath() {
        return getProperty("ChromeDriverPath", chromeDriverPath);
    }

    public String getIEDriverPath() {
        return getProperty("IEDriverPath", "./lib/Drivers/IEDriverServer.exe");
    }

    public String getEdgeDriverPath() {
        return getProperty("EdgeDriverPath", "./lib/Drivers/MicrosoftWebDriver.exe");
    }
    // End of driverpath related methods

    //Getters for some specific Properties. These methods extracts the properties based
    // on currently loaded API configurations.
    public Boolean useProxy() {
        return Boolean.valueOf(getUseProxy());
    }
    
    public String getUseProxy() {
        return apiConfigFilePropMap.get(currLoadedAPIConfig).getProperty("useProxy", "false");
    }

    public String getProxyHost() {
        return apiConfigFilePropMap.get(currLoadedAPIConfig).getProperty("proxyHost", "");
    }

    public String getProxyPort() {
        return apiConfigFilePropMap.get(currLoadedAPIConfig).getProperty("proxyPort", "");
    }
   
    public String getSSLCertificateVerification() {
        return apiConfigFilePropMap.get(currLoadedAPIConfig).getProperty("sslCertificateVerification", "false");
    }
    
    public Boolean sslCertificateVerification() {
        return Boolean.valueOf(getSSLCertificateVerification());
    }  

    public String getSelfSigned() {
        return apiConfigFilePropMap.get(currLoadedAPIConfig).getProperty("selfSigned", "false");
    }
    
    public Boolean selfSigned() {
        return Boolean.valueOf(getSelfSigned());
    }  
 
    public String getKeyStorePath() {
        return apiConfigFilePropMap.get(currLoadedAPIConfig).getProperty("keyStorePath", "");
    }

    public String getKeyStorePassword() {
        return apiConfigFilePropMap.get(currLoadedAPIConfig).getProperty("keyStorePassword", "");
    }

    /**
     * Retrieves the configured HTTP redirect policy for the currently loaded API configuration.
     * <p>
     * This method looks up the property <code>httpClientRedirect</code> inside the API configuration
     * identified by {@code currLoadedAPIConfig}. If the property is not defined, the method returns the
     * default value <code>"NEVER"</code>.
     * </p>
     *
     * @return the redirect policy defined for the current API configuration, or <code>"NEVER"</code>
     *         if the property is missing
     */
    public String getHttpClientRedirect() {
        return apiConfigFilePropMap.get(currLoadedAPIConfig).getProperty("httpClientRedirect", "NEVER");
    }

      //Setters for some specific properties.
      //Commented out as these are not set programmatically but are extracted from
      //configurations files.
//    public void setKeyStorePassword(String value) {
//        setProperty("keyStorePassword", value);
//    }
//    
//    public void setKeyStorePath(String path) {
//        setProperty("keyStorePath", path);
//    }
//    
//    public void setSelfSigned(String value) {
//        setProperty("selfSigned", value);
//    }
//
//    public void setSSLCertificateVerification(String value) {
//        setProperty("sslCertificateVerification", value);
//    }
//
//    public void setProxyPort(String value) {
//        setProperty("proxyPort", value);
//    }
//
//    public void setProxyHost(String value) {
//        setProperty("proxyHost", value);
//    }
//
//    public void setUseProxy(String value) {
//        setProperty("useProxy", value);
//    }
}
