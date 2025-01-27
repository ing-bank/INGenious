
package com.ing.datalib.settings;

public class DriverSettings extends AbstractPropSettings {

    private static String geckoDriverPath, chromeDriverPath;

    static {
        setDriverPath();
    }

    public DriverSettings(String location) {
        super(location, "DriverSettings");
        if (isEmpty()) {
            loadDefault();
        }
    }

    private void loadDefault() {
        setSSLCertVerification(getSSLCertVerification());
        setUseProxy(getUseProxy());
        setProxyHost(getProxyHost());
        setProxyPort(getProxyPort());
    }

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

    public Boolean useProxy() {
        return Boolean.valueOf(getProperty("useProxy", "false"));
    }
    
    
    public void setUseProxy(String value) {
        setProperty("useProxy", value);
    }

    public String getUseProxy() {
        return getProperty("useProxy", "false");
    }

    public void setProxyHost(String value) {
        setProperty("proxyHost", value);
    }

    public String getProxyHost() {
        return getProperty("proxyHost", "");
    }

    public void setProxyPort(String value) {
        setProperty("proxyPort", value);
    }

    public String getProxyPort() {
        return getProperty("proxyPort", "");
    }
    
    public void setSSLCertVerification(String value) {
        setProperty("setSSLCertVerification", value);
    }

    public String getSSLCertVerification() {
        return getProperty("setSSLCertVerification", "false");
    }
    
    public Boolean sslCertificateVerification() {
        return Boolean.valueOf(getProperty("sslCertificateVerification", "false"));
    }


}
