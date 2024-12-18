
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

    private final DriverSettings driverSettings;
    private final Capabilities capabilities;
    private final Emulators emulators;
    private final TestMgmtModule testMgmtModule;

    private final MailSettings mailSettings;
    
    //private final DatabaseSettings dbSettings;
    
    private final ReportPortalSettings rpSettings;
    
    private final ExtentReportSettings extentSettings;

   // private final ContextSettings contextSettings;

    private final ExecutionSettings execSettings;

    private final DBProperties dbProperties;
    private final ContextOptions contextOptions;

    public ProjectSettings(Project sProject) {
        this.sProject = sProject;
        this.userDefinedSettings = new UserDefinedSettings(getLocation());
        this.driverSettings = new DriverSettings(getLocation());
        this.capabilities = new Capabilities(getLocation());
        this.emulators = new Emulators(getLocation());
        this.testMgmtModule = new TestMgmtModule(getLocation());
        this.execSettings = new ExecutionSettings(getLocation());
        this.mailSettings = new MailSettings(getLocation());
        //this.dbSettings = new DatabaseSettings(getLocation());
        this.rpSettings = new ReportPortalSettings(getLocation());
        this.extentSettings = new ExtentReportSettings(getLocation());
       // this.contextSettings = new ContextSettings(getLocation());
        this.dbProperties = new DBProperties(getLocation());
        this.contextOptions = new ContextOptions(getLocation());


    }

    public void resetLocation() {
        userDefinedSettings.setLocation(getLocation());
        driverSettings.setLocation(getLocation());
        capabilities.setLocation(getLocation());
        emulators.setLocation(getLocation());
        testMgmtModule.setLocation(getLocation());
        execSettings.setLocation(getLocation());
        mailSettings.setLocation(getLocation());
        //dbSettings.setLocation(getLocation());
        rpSettings.setLocation(getLocation());
        extentSettings.setLocation(getLocation());
        //contextSettings.setLocation(getLocation());
        dbProperties.setLocation(getLocation());
        contextOptions.setLocation(getLocation());
    }

    public final String getLocation() {
        return sProject.getLocation() + File.separator + "Settings";
    }

    public Project getProject() {
        return sProject;
    }

    public MailSettings getMailSettings() {
        return mailSettings;
    }
    
    /*public DatabaseSettings getDatabaseSettings(){
        return dbSettings;
    }*/
    
    public ReportPortalSettings getRPSettings(){
        return rpSettings;
    }
    
    public ExtentReportSettings getExtentSettings(){
        return extentSettings;
    }

   /* public ContextSettings getContextSettings(){
        return contextSettings;
    }*/
    
    public DriverSettings getDriverSettings() {
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

    public DBProperties getDBProperty() {
        return dbProperties;
    }

    public ContextOptions getContextOption() {
        return contextOptions;
    }

    public void save() {
        userDefinedSettings.save();
        execSettings.save();
        driverSettings.save();
        emulators.save();
        capabilities.save();
        testMgmtModule.save();
        mailSettings.save();
       // dbSettings.save();
        extentSettings.save();
        //contextSettings.save();
        dbProperties.save();
        contextOptions.save();
    }
}
