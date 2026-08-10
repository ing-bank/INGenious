package com.ing.datalib.settings;

//# Deprecated as of Sept 2025
//# Reason: Replaced by new DriverProperties in `Datalib module`
//# Retained for reference in case rollback is needed
//# Dev : Renz
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
        setSSLCertificateVerification(getSSLCertificateVerification());
        setUseProxy(getUseProxy());
        setProxyHost(getProxyHost());
        setProxyPort(getProxyPort());
        setSelfSigned(getSelfSigned());
        setKeyStorePath(getKeyStorePath());
        setKeyStorePassword(getKeyStorePassword());
    }

    private static void setDriverPath() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
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

    public void setSSLCertificateVerification(String value) {
        setProperty("sslCertificateVerification", value);
    }

    public String getSSLCertificateVerification() {
        return getProperty("sslCertificateVerification", "false");
    }

    public Boolean sslCertificateVerification() {
        return Boolean.valueOf(getProperty("sslCertificateVerification", "false"));
    }

    public void setSelfSigned(String value) {
        setProperty("selfSigned", value);
    }

    public String getSelfSigned() {
        return getProperty("selfSigned", "false");
    }

    public Boolean selfSigned() {
        return Boolean.valueOf(getProperty("selfSigned", "false"));
    }

    public void setKeyStorePath(String path) {
        setProperty("keyStorePath", path);
    }

    public String getKeyStorePath() {
        return getProperty("keyStorePath", "");
    }

    public void setKeyStorePassword(String value) {
        setProperty("keyStorePassword", value);
    }

    public String getKeyStorePassword() {
        return getProperty("keyStorePassword", "");
    }
}
