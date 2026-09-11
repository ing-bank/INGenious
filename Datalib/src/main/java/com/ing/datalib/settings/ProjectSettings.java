package com.ing.datalib.settings;

import com.ing.datalib.component.Project;
import com.ing.datalib.settings.emulators.Device;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.datalib.settings.migration.EmulatorToDeviceMigration;
import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.util.logging.Logger;

/**
 *
 *
 */
public class ProjectSettings {
    private final Project sProject;

    private final UserDefinedSettings userDefinedSettings;

    private final DriverProperties driverSettings;
    // private final DriverSettings driverSettings;
    private final Capabilities capabilities;
    private final Emulators emulators;
    private final Devices devices;
    private boolean readOnlyMode = false;
    private final TestMgmtModule testMgmtModule;
    private final ReportPortalSettings rpSettings;
    private final ExtentReportSettings extentSettings;
    private final ExecutionSettings execSettings;
    private final DBProperties dbSettings;
    private final ContextOptions contextSettings;
    private final KafkaSSLConfigurations SSLConfigurations;
    private final LambdaTestCaps lambdaTestCaps;
    private final SapConnections sapConnections;
    private final SapDefaults sapDefaults;
    private final SapConfigRegistry sapConfigRegistry;

    private static final Logger LOGGER = Logger.getLogger(ProjectSettings.class.getName());

    public ProjectSettings(Project sProject) {
        this(sProject, false);
    }

    public ProjectSettings(Project sProject, boolean readOnlyMode) {
        this.sProject = sProject;
        this.readOnlyMode = readOnlyMode;
        this.userDefinedSettings = new UserDefinedSettings(getLocation());
        // this.driverSettings = new DriverSettings(getLocation());
        this.driverSettings = new DriverProperties(getLocation());
        this.capabilities = new Capabilities(getLocation(), readOnlyMode);
        this.emulators = new Emulators(getLocation(), readOnlyMode);
        this.devices = new Devices(getLocation(), readOnlyMode);
        this.testMgmtModule = new TestMgmtModule(getLocation());
        this.execSettings = new ExecutionSettings(getLocation());
        this.dbSettings = new DBProperties(getLocation());
        this.rpSettings = new ReportPortalSettings(getLocation());
        this.extentSettings = new ExtentReportSettings(getLocation());
        this.contextSettings = new ContextOptions(getLocation());
        this.SSLConfigurations = new KafkaSSLConfigurations(getLocation());
        this.lambdaTestCaps = new LambdaTestCaps(getLocation());
        this.sapConnections = new SapConnections(getLocation(), readOnlyMode);
        this.sapDefaults = new SapDefaults(getLocation());
        this.sapConfigRegistry = new SapConfigRegistry(sapConnections, sapDefaults);

        // Ensure the project has at least one SAP connection + a default
        // (skipped if read-only). Migrates the legacy single "SAP" config.
        ensureSapDefaultConnection();

        // One-time migration: move legacy "Manage Browsers" emulator entries
        // into the new "Manage Devices" store. Idempotent and SAP-preserving.
        // Skipped if in read-only mode.
        if (!readOnlyMode) {
            EmulatorToDeviceMigration.migrate(emulators, devices);
        }
    }

    /**
     * Ensures the project has at least one SAP connection under {@code Settings/SAP/}
     * plus a default pointer. On first run for a legacy project this migrates the old
     * single config: {@code Settings/Capabilities/SAP.properties}, else the even older
     * {@code Settings/SAP.properties}, else a blank starter template. The legacy
     * {@code Emulators.json} "SAP" row is left untouched but no longer consulted.
     * Skipped in read-only mode.
     */
    private void ensureSapDefaultConnection() {
        if (readOnlyMode) {
            return;
        }
        if (!sapConnections.getSapList().isEmpty()) {
            if (sapDefaults.getProperty(SapDefaults.KEY_MODEL) == null) {
                sapDefaults.setModel(SapDefaults.MODEL_LEGACY);
                sapDefaults.save();
            }
            return;
        }

        LinkedProperties seed = new LinkedProperties();
        seed.putAll(sapConnections.defaultConnectionProperties());

        LinkedProperties legacyCaps = capabilities.getCapabiltiesFor("SAP");
        File legacyRoot = new File(getLocation() + File.separator + "SAP.properties");
        if (legacyCaps != null) {
            copyIfPresent(legacyCaps, seed, "app", "connectionName");
        } else if (legacyRoot.exists()) {
            LinkedProperties root = PropUtils.load(legacyRoot);
            copyIfPresent(root, seed, "app", "connectionName");
            LOGGER.warning(
                "Migrating deprecated Settings/SAP.properties to Settings/SAP/SAP.properties"
            );
        }

        sapConnections.addSap("SAP", seed);
        sapDefaults.setDefaultConnection("SAP");
        sapDefaults.setModel(SapDefaults.MODEL_LEGACY);
        sapDefaults.save();
    }

    private static void copyIfPresent(
        java.util.Properties from,
        java.util.Properties to,
        String... keys
    ) {
        for (String key : keys) {
            String v = from.getProperty(key);
            if (v != null && !v.trim().isEmpty()) {
                to.setProperty(key, v);
            }
        }
    }

    public void resetLocation() {
        userDefinedSettings.setLocation(getLocation());
        // driverSettings.setLocation(getLocation());
        driverSettings.setLocation(getLocation());
        capabilities.setLocation(getLocation());
        emulators.setLocation(getLocation());
        devices.setLocation(getLocation());
        testMgmtModule.setLocation(getLocation());
        execSettings.setLocation(getLocation());
        dbSettings.setLocation(getLocation());
        rpSettings.setLocation(getLocation());
        extentSettings.setLocation(getLocation());
        contextSettings.setLocation(getLocation());
        lambdaTestCaps.setLocation(getLocation());
        sapConnections.setLocation(getLocation());
        sapDefaults.setLocation(getLocation());
    }

    public final String getLocation() {
        return sProject.getLocation() + File.separator + "Settings";
    }

    public Project getProject() {
        return sProject;
    }

    public DBProperties getDatabaseSettings() {
        return dbSettings;
    }

    public ReportPortalSettings getRPSettings() {
        return rpSettings;
    }

    public ExtentReportSettings getExtentSettings() {
        return extentSettings;
    }

    public KafkaSSLConfigurations getKafkaSSLConfigurations() {
        return SSLConfigurations;
    }

    public ContextOptions getContextSettings() {
        return contextSettings;
    }

    public DriverProperties getDriverSettings() {
        return driverSettings;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public Emulators getEmulators() {
        return emulators;
    }

    public Devices getDevices() {
        return devices;
    }

    /**
     * Resolves the Remote URL / Appium endpoint for the given browser-or-device
     * name. Falls back from Emulators (legacy) to Devices (new Manage Devices
     * tab) so the driver factory works for entries from either source.
     *
     * @return the configured URL, or {@code null} if none is found.
     */
    public String resolveRemoteUrl(String name) {
        Emulator e = emulators.getEmulator(name);
        if (e != null && e.getRemoteUrl() != null && !e.getRemoteUrl().isEmpty()) {
            return e.getRemoteUrl();
        }
        Device d = devices.getDevice(name);
        if (d != null && d.getRemoteUrl() != null && !d.getRemoteUrl().isEmpty()) {
            return d.getRemoteUrl();
        }
        return null;
    }

    public TestMgmtModule getTestMgmtModule() {
        return testMgmtModule;
    }

    public ExecutionSettings getExecSettings() {
        return execSettings;
    }

    public ExecutionSettings getExecSettings(String release, String testset) {
        return sProject.getReleaseByName(release).getTestSetByName(testset).getExecSettings();
    }

    public UserDefinedSettings getUserDefinedSettings() {
        return userDefinedSettings;
    }

    public LambdaTestCaps getLambdaTestCaps() {
        return lambdaTestCaps;
    }

    public SapConnections getSapConnections() {
        return sapConnections;
    }

    public SapDefaults getSapDefaults() {
        return sapDefaults;
    }

    public SapConfigRegistry getSapConfigRegistry() {
        return sapConfigRegistry;
    }

    public void save() {
        userDefinedSettings.save();
        execSettings.save();
        driverSettings.save();
        emulators.save();
        devices.save();
        capabilities.save();
        testMgmtModule.save();
        dbSettings.save();
        extentSettings.save();
        contextSettings.save();
        SSLConfigurations.save();
        lambdaTestCaps.save();
        sapConnections.save();
        sapDefaults.save();
    }
}
