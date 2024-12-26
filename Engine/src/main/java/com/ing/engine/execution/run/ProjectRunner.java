
package com.ing.engine.execution.run;

import com.ing.datalib.component.EnvTestData;
import com.ing.datalib.component.ExecutionStep;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.settings.ExecutionSettings;
import com.ing.engine.constants.FilePath;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.RunManager;
import com.ing.engine.execution.data.DataIterator;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class ProjectRunner implements TestRunner {

    Project sProject;
    static Map<String, DataIterator> resolvedIterters = new HashMap<>();

    private int retryCount = 0;

    private static void initNewRun() {
        resolvedIterters.clear();
    }

    public static ProjectRunner load(String projLocation) {

        ProjectRunner runner = new ProjectRunner();
        runner.setProject(projLocation);
        initNewRun();
        return runner;
    }

    public static ProjectRunner load(Project project) {
        ProjectRunner runner = new ProjectRunner();
        runner.setProject(project);
        initNewRun();
        return runner;
    }

    @Override
    public Project getProject() {
        return sProject;
    }

    public void setProject(String project) {
        setProject(new Project(project));
    }

    public void setProject(Project project) {
        sProject = project;
        upadteProperties();
        overrideWithEnv();
        setRetryCount();
    }

    /**
     * env if given else def env
     *
     * @see ExecProperties#TestEnv
     * @return the current test env
     */
    @Override
    public String runEnv() {
        return Objects.toString(getExecSettings().getRunSettings().getTestEnv(),
                dataProvider().defEnv());
    }

    @Override
    public EnvTestData dataProvider() {
        return getProject().getTestData();
    }

    public TestSet getTestSet() {
        String release = RunManager.getGlobalSettings().getRelease();
        String testset = RunManager.getGlobalSettings().getTestSet();
        return sProject.getReleaseByName(release).getTestSetByName(testset);
    }

    public synchronized DataIterator getIterater(TestCase testcase) {
        return getIterater(testcase.getKey());
    }

    public synchronized DataIterator getIterater(String scenario, String testcase) {
        return getIterater(scenario + "#" + testcase);
    }

    public synchronized DataIterator getIterater(String key) {
        if (!resolvedIterters.containsKey(key)) {
            resolvedIterters.put(key, new DataIterator());
        }
        return resolvedIterters.get(key);
    }

    public boolean isDebugExe() {
        return RunManager.getGlobalSettings().isTestRun()
                && SystemDefaults.debugMode.get();
    }

    @Override
    public boolean isContinueOnError() {
        return getExecSettings().getRunSettings().getIterationMode().equals("ContinueOnError");
    }

    public boolean useExistingBrowser() {
        return getExecSettings().getRunSettings().useExistingDriver();
    }

    public ExecutionSettings getExecSettings() {
        if (RunManager.getGlobalSettings().isTestRun()) {
            return sProject.getProjectSettings().getExecSettings();
        } else {
            return getTestSet().getExecSettings();
        }
    }

    private void setRetryCount() {
        if (RunManager.getGlobalSettings().isTestRun()) {
            retryCount = 0;
        } else {
            retryCount = getExecSettings().getRunSettings().getRerunTimes();
        }
    }

    public void afterExecution(Boolean passed) {
        if (passed) {
            retryCount = 0;
        } else {
            modifyTestSet();
        }
    }

    private void modifyTestSet() {
        if (retryCount > 0) {
            System.out.println("\n Retrying Execution \n");
            for (ExecutionStep step : getTestSet().getTestSteps()) {
                if (Boolean.valueOf(step.getExecute())
                        && "passed".equalsIgnoreCase(step.getStatus())) {
                    step.setExecute("false");
                }
            }
        }
    }

    public Boolean retryExecution() {
        return retryCount-- > 0;
    }

    private void overrideWithEnv() {
        Map<String, String> prop = new LinkedHashMap<>();
        /*
         * get the env settings from SET app.* in command line or terminal
         * in java words Environment variables
         */
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            if (e.getKey().startsWith("app.")) {
                prop.put(e.getKey().replace("app.", ""), e.getValue());
            }
        }
        /**
         * update with app CLI's setEnv settings (will override the System Env)
         */

        prop.putAll(SystemDefaults.EnvVars);
        if (!prop.isEmpty()) {
            /*
            * display entries only if debug flag is set
            */
            System.out.println("Override with Environment Settings :\n "
                    + (SystemDefaults.debug() ? prop.entrySet() : prop.keySet()));
            /*
             * update the exe/run/user settings with CLI's Env settings
             * (case sensitive)
             */
            prop.entrySet().stream().forEach((e) -> {
                try {
                    String key = e.getKey(), value = e.getValue();
                    if (key.startsWith("run.")) {
                        getExecSettings().getRunSettings().put(
                                key.replace("run.", ""), value);
                    } else if (key.startsWith("exe.")) {
                        RunManager.getGlobalSettings().put(
                                key.replace("exe.", ""), value);
                    } else if (key.startsWith("user.")) {
                        getProject().getProjectSettings().getUserDefinedSettings().put(
                                key.replace("user.", ""), value);
                    }else if (key.startsWith("tm.")) {
                        getExecSettings().getTestMgmgtSettings().put(
                                key.replace("tm.", ""), value);
                    } else if (key.startsWith("driver.")) {
                        getProject().getProjectSettings().getDriverSettings().put(
                                key.replace("driver.", ""), value);
                    } else if(key.startsWith("capability.")){
                        String args[] = key.split("\\.");
                        String browser = capitalizeFirstLetter(args[1]);
                        String capability = args[2];
                        getProject().getProjectSettings().getCapabilities().getCapabiltiesFor(browser).update(capability,value);
                    }else if (key.startsWith("db.alias@")) {
                        String args[] = key.split("\\.");
                     //   String db = args[1];
                     //   getProject().getProjectSettings().getDatabaseSettings().getDBPropertiesFor(db).update(db, value);
                    } else if (key.startsWith("context.alias@")) {
                        String args[] = key.split("\\.");
                     //   String context = args[1];
                     //   getProject().getProjectSettings().getContextSettings().getContextOptionsFor(context).update(context,value);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(ProjectRunner.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    private void upadteProperties() {
        File appSettings = new File(FilePath.getAppSettings());
        if (appSettings.exists()) {
            try {
                Properties appSett = new Properties();
                appSett.load(new FileReader(appSettings));
                System.getProperties().putAll(appSett);
            } catch (IOException ex) {
                Logger.getLogger(ProjectRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
