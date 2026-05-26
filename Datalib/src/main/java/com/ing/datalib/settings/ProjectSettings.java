
package com.ing.datalib.settings;

import com.ing.datalib.component.Project;

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
    private final TestMgmtModule testMgmtModule;
    private final ReportPortalSettings rpSettings;    
    private final ExtentReportSettings extentSettings;
    private final ExecutionSettings execSettings;   
    private final DBProperties dbSettings;
    private final ContextOptions contextSettings;
    private final KafkaSSLConfigurations SSLConfigurations;
    private final LambdaTestCaps lambdaTestCaps;

    public ProjectSettings(Project sProject) {
        this.sProject = sProject;
        this.userDefinedSettings = new UserDefinedSettings(getLocation());
        // this.driverSettings = new DriverSettings(getLocation());
        this.driverSettings = new DriverProperties(getLocation());
        this.capabilities = new Capabilities(getLocation());
        this.emulators = new Emulators(getLocation());
        this.testMgmtModule = new TestMgmtModule(getLocation());
        this.execSettings = new ExecutionSettings(getLocation());
        this.dbSettings = new DBProperties(getLocation());
        this.rpSettings = new ReportPortalSettings(getLocation());
        this.extentSettings = new ExtentReportSettings(getLocation());
        this.contextSettings = new ContextOptions(getLocation());
        this.SSLConfigurations = new KafkaSSLConfigurations(getLocation());
        this.lambdaTestCaps = new LambdaTestCaps(getLocation());
        
        // Ensure SAP is available as default browser
        ensureSAPDefaultEmulator();
    }
    
    /**
     * Ensures SAP emulator exists for this project. 
     * Adds SAP if missing and saves configuration.
     * Creates SAP.properties file if it doesn't exist.
     */
    private void ensureSAPDefaultEmulator() {
        // Always ensure SAP exists (regardless of file existence - works for new projects)
        if (emulators.getEmulator("SAP") == null) {
            emulators.addEmulator("SAP");
            emulators.save();
        }
        
        // Ensure SAP.properties file exists
        capabilities.ensureSAPCapabilitiesExist();
    }

    public void resetLocation() {
        userDefinedSettings.setLocation(getLocation());
        // driverSettings.setLocation(getLocation());
        driverSettings.setLocation(getLocation());
        capabilities.setLocation(getLocation());
        emulators.setLocation(getLocation());
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

    
    public DBProperties getDatabaseSettings(){
        return dbSettings;
    }
    
    public ReportPortalSettings getRPSettings(){
        return rpSettings;
    }
    
    public ExtentReportSettings getExtentSettings(){
        return extentSettings;
    }
    
    public KafkaSSLConfigurations getKafkaSSLConfigurations(){
        return SSLConfigurations;
    }

    public ContextOptions getContextSettings(){
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
    
    public LambdaTestCaps getLambdaTestCaps(){
        return lambdaTestCaps;
    }
    

    public void save() {
        userDefinedSettings.save();
        execSettings.save();
        driverSettings.save();
        emulators.save();
        capabilities.save();
        testMgmtModule.save();
        dbSettings.save();
        extentSettings.save();
        contextSettings.save();
        SSLConfigurations.save();
        lambdaTestCaps.save();
    }
}
