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
        return Objects.toString(
            getExecSettings().getRunSettings().getTestEnv(),
            dataProvider().defEnv()
        );
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
        return RunManager.getGlobalSettings().isTestRun() && SystemDefaults.debugMode.get();
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
                if (
                    Boolean.valueOf(step.getExecute()) &&
                    "passed".equalsIgnoreCase(step.getStatus())
                ) {
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
            System.out.println(
                "Override with Environment Settings :\n " +
                (SystemDefaults.debug() ? prop.entrySet() : prop.keySet())
            );
            /*
             * update the exe/run/user settings with CLI's Env settings
             * (case sensitive)
             */
            prop
                .entrySet()
                .stream()
                .forEach(
                    e -> {
                        try {
                            applyOverride(e.getKey(), e.getValue());
                        } catch (Exception ex) {
                            Logger
                                .getLogger(ProjectRunner.class.getName())
                                .log(Level.SEVERE, null, ex);
                        }
                    }
                );
        }
    }

    /**
     * Apply a single {@code -setEnv "key=value"} (or {@code app.key=value} OS
     * env) override against the in-memory {@link com.ing.datalib.settings.ProjectSettings}
     * model. The dispatcher recognises a fixed set of prefixes — see
     * {@link #PREFIX_CATALOGUE} (printable via {@code ingenious config prefixes}).
     *
     * <p>Unknown prefixes are ignored silently (consistent with the previous
     * behaviour) so that user-defined keys don't fail the run.</p>
     */
    private void applyOverride(String key, String value) {
        if (key.startsWith("run.")) {
            getExecSettings().getRunSettings().put(key.replace("run.", ""), value);
        } else if (key.startsWith("exe.")) {
            RunManager.getGlobalSettings().put(key.replace("exe.", ""), value);
        } else if (key.startsWith("user.")) {
            getProject()
                .getProjectSettings()
                .getUserDefinedSettings()
                .put(key.replace("user.", ""), value);
        } else if (key.startsWith("tm.")) {
            getExecSettings().getTestMgmgtSettings().put(key.replace("tm.", ""), value);
        } else if (key.startsWith("driver.")) {
            getProject()
                .getProjectSettings()
                .getDriverSettings()
                .put(key.replace("driver.", ""), value);
        } else if (key.startsWith("capability.")) {
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            String browser = capitalizeFirstLetter(args[1]);
            String capability = args[2];
            getProject()
                .getProjectSettings()
                .getCapabilities()
                .getOrCreateCapabiltiesFor(browser)
                .update(capability, value);
        } else if (key.startsWith("db.")) {
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            String db = args[1];
            String property = args[2];
            getProject()
                .getProjectSettings()
                .getDatabaseSettings()
                .getDBPropertiesFor(db)
                .put(property, value);
        } else if (key.startsWith("context.")) {
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            String context = args[1];
            String property = args[2];
            getProject()
                .getProjectSettings()
                .getContextSettings()
                .getContextOptionsFor(context)
                .put(property, value);
        } else if (key.startsWith("kafkaSSl.") || key.startsWith("kafkaSsl.")) {
            // §C-3: accept both legacy ("kafkaSSl") and canonical ("kafkaSsl")
            // spellings. The legacy spelling is preserved for back-compat.
            String args[] = key.split("\\.", 2);
            if (args.length < 2) return;
            String capability = args[1];
            getProject().getProjectSettings().getKafkaSSLConfigurations().put(capability, value);
        } else if (key.startsWith("api.")) {
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            String api = args[1];
            String property = args[2];
            getProject()
                .getProjectSettings()
                .getDriverSettings()
                .getAPIPropertiesFor(api)
                .put(property, value);
        } else if (key.startsWith("lambdatest.")) {
            // §B-1: LambdaTest Grid Capabilities
            getProject()
                .getProjectSettings()
                .getLambdaTestCaps()
                .put(key.replace("lambdatest.", ""), value);
        } else if (key.startsWith("browserArg.")) {
            // §B-2: per-browser launch flags. Stored as indexed properties
            // (`arg.<n>`) in the browser's capability bag so existing
            // capability-aware launcher code can pick them up.
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            String browser = capitalizeFirstLetter(args[1]);
            String index = args[2];
            getProject()
                .getProjectSettings()
                .getCapabilities()
                .getOrCreateCapabiltiesFor(browser)
                .update("arg." + index, value);
        } else if (key.startsWith("browser.")) {
            // §B-2: arbitrary per-browser key (binary path, enabled flag,
            // anything that lives in the browser's capability bag). Acts as
            // a create-on-missing alias of `capability.<browser>.<key>`.
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            String browser = capitalizeFirstLetter(args[1]);
            String prop = args[2];
            getProject()
                .getProjectSettings()
                .getCapabilities()
                .getOrCreateCapabiltiesFor(browser)
                .update(prop, value);
        } else if (key.startsWith("device.")) {
            // §B-3: Manage Devices. Special keys `RemoteURL`, `LambdaTest`,
            // `__enabled` route to the device's typed fields; everything
            // else lands in the device's capability map (same store used by
            // the Mobile/Appium runner today).
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            String name = args[1];
            String prop = args[2];
            com.ing.datalib.settings.emulators.Device d = getProject()
                .getProjectSettings()
                .getDevices()
                .getOrCreateDevice(name);
            if ("RemoteURL".equalsIgnoreCase(prop) || "Remote URL".equalsIgnoreCase(prop)) {
                d.setRemoteUrl(value);
            } else if ("LambdaTest".equalsIgnoreCase(prop)) {
                d.setLambdaTest(Boolean.parseBoolean(value));
            } else if ("__enabled".equalsIgnoreCase(prop)) {
                // create-on-missing already happened above; nothing else to do
            } else {
                getProject()
                    .getProjectSettings()
                    .getCapabilities()
                    .getOrCreateCapabiltiesFor(name)
                    .update(prop, value);
            }
        } else if (key.startsWith("emulator.")) {
            // §B-3 alias: writes route through the same Devices model — the
            // legacy "Emulators" tab has been phased out (see §16 of the
            // change log) and existing entries are migrated to Devices on
            // load. New `-setEnv "emulator.…"` values land in Devices too.
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            applyOverride("device." + args[1] + "." + args[2], value);
        } else if (key.startsWith("tmModule.")) {
            // §B-4: AzureDevOps TestPlan — per-module options.
            // `tmModule.<module>.<key>=<value>` creates the module on first
            // use and sets / replaces the option in TestMgModule.getOptions().
            // The special key `__enabled=true` just triggers create.
            String args[] = key.split("\\.", 3);
            if (args.length < 3) return;
            String module = args[1];
            String optName = args[2];
            getProject().getProjectSettings().getTestMgmtModule().setOption(module, optName, value);
        }
    }

    /**
     * Catalogue of recognised override prefixes, exposed via
     * {@code ingenious config prefixes}. Each entry is
     * {@code prefix => human-readable description}.
     *
     * <p>Order matches the documentation in
     * {@code CLI_Override_Plan_And_Usage.md}.</p>
     */
    public static final java.util.LinkedHashMap<String, String> PREFIX_CATALOGUE;

    static {
        PREFIX_CATALOGUE = new java.util.LinkedHashMap<>();
        PREFIX_CATALOGUE.put(
            "exe.<key>",
            "Engine-wide GlobalSettings (Settings → Run Settings, global scope)"
        );
        PREFIX_CATALOGUE.put("run.<key>", "Per-execution RunSettings (Settings → Run Settings)");
        PREFIX_CATALOGUE.put("user.<key>", "UserDefined variables (Settings → UserDefined)");
        PREFIX_CATALOGUE.put("tm.<key>", "Flat Test Manager settings (Settings → TM Settings)");
        PREFIX_CATALOGUE.put(
            "driver.<key>",
            "Driver / Launch Configurations (Archetype → Launch Configurations)"
        );
        PREFIX_CATALOGUE.put(
            "capability.<browser>.<key>",
            "Per-browser capability override (Archetype → Manage Browsers → Capabilities/Options)"
        );
        PREFIX_CATALOGUE.put(
            "db.<alias>.<key>",
            "Database connection property (Archetype → Database Configurations)"
        );
        PREFIX_CATALOGUE.put(
            "context.<alias>.<key>",
            "Context configuration property (Archetype → Context Configurations)"
        );
        PREFIX_CATALOGUE.put(
            "api.<alias>.<key>",
            "API configuration property (Archetype → API Configurations)"
        );
        PREFIX_CATALOGUE.put(
            "kafkaSsl.<key>",
            "Kafka SSL configuration (Archetype → Kafka SSL Configurations). " +
            "Legacy spelling 'kafkaSSl' is still accepted."
        );
        PREFIX_CATALOGUE.put(
            "lambdatest.<key>",
            "LambdaTest Grid Capabilities (Settings → LambdaTest Grid Capabilities)"
        );
        PREFIX_CATALOGUE.put(
            "browser.<browser>.<key>",
            "Per-browser arbitrary property — create-on-missing alias of 'capability.*'"
        );
        PREFIX_CATALOGUE.put(
            "browserArg.<browser>.<n>",
            "Indexed per-browser launch flag (writes 'arg.<n>' into the browser's capability bag)"
        );
        PREFIX_CATALOGUE.put(
            "device.<name>.<key>",
            "Mobile device entry (Archetype → Manage Devices). " +
            "Reserved keys: RemoteURL, LambdaTest, __enabled. " +
            "Anything else lands in the device's capability map."
        );
        PREFIX_CATALOGUE.put(
            "emulator.<name>.<key>",
            "Alias of 'device.<name>.<key>' — kept for back-compat with the phased-out Emulators tab."
        );
        PREFIX_CATALOGUE.put(
            "tmModule.<module>.<key>",
            "AzureDevOps TestPlan per-module option. Creates the module on first use. " +
            "Reserved key: __enabled."
        );
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
