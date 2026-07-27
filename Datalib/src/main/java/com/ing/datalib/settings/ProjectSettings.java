package com.ing.datalib.settings;

import com.ing.datalib.component.Project;
import com.ing.datalib.settings.emulators.Device;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.datalib.settings.migration.EmulatorToDeviceMigration;
import java.io.File;

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
    private final RecorderSettings recorderSettings;
    private final KafkaSSLConfigurations SSLConfigurations;
    private final LambdaTestCaps lambdaTestCaps;

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
        this.recorderSettings = new RecorderSettings(getLocation());
        this.SSLConfigurations = new KafkaSSLConfigurations(getLocation());
        this.lambdaTestCaps = new LambdaTestCaps(getLocation());

        // Ensure SAP is available as default browser (skipped if read-only)
        ensureSAPDefaultEmulator();

        // One-time migration: move legacy "Manage Browsers" emulator entries
        // into the new "Manage Devices" store. Idempotent and SAP-preserving.
        // Skipped if in read-only mode.
        if (!readOnlyMode) {
            EmulatorToDeviceMigration.migrate(emulators, devices);
        }
    }

    /**
     * Ensures SAP emulator exists for this project.
     * Adds SAP if missing and saves configuration.
     * Creates SAP.properties file if it doesn't exist.
     * Skipped if in read-only mode (e.g., during validation).
     */
    private void ensureSAPDefaultEmulator() {
        if (!readOnlyMode) {
            // Always ensure SAP exists (regardless of file existence - works for new projects)
            if (emulators.getEmulator("SAP") == null) {
                emulators.addEmulator("SAP");
                emulators.save();
            }

            // Ensure SAP.properties file exists
            capabilities.ensureSAPCapabilitiesExist();
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

    /**
     * Recorder settings for this project, such as the page a recording starts on.
     *
     * @return the recorder settings, never {@code null}
     */
    public RecorderSettings getRecorderSettings() {
        return recorderSettings;
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
    }
}
