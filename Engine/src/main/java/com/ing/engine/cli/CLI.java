
package com.ing.engine.cli;

import com.ing.datalib.util.data.FileScanner;
import com.ing.engine.cli.lib.Performance;
import com.ing.engine.constants.FilePath;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunManager;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.reporting.util.RDS;
import com.ing.engine.settings.GlobalSettings;
import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 *
 */
public class CLI {

    private static final Logger LOG = Logger.getLogger(CLI.class.getName());

    static void t() {
        System.out.println(new Date().toString());
    }

    static void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setDescPadding(0);
        formatter.setOptionComparator(null);
        System.out.println("CLI\n"
                + "Invoke command line options  for retrieving execution details, setting variable, change settings etc.");
        formatter.printHelp("\nRun.bat", LookUp.OPTIONS, true);
    }

    static void setVar(String val) {
        if (val.contains("=")) {
            String[] vals = val.split("=", 2);
            SystemDefaults.CLVars.put(vals[0], vals[1]);
        } else {
            /*
            * handle as a flag if it doesn't match key=var
             */
            SystemDefaults.CLVars.put(val, "true");
        }
    }

    static void setEnv(String val) {
        if (val.contains(";")) {
            for (String v : val.split(";")) {
                addEnv(v);
            }
        } else {
            addEnv(val);
        }
    }

    private static void addEnv(String val) {
        String key = val.substring(0, val.indexOf('='));
        val = val.substring(val.indexOf('=') + 1);
        SystemDefaults.EnvVars.put(key, val);
    }

    static void Default(Option op) {
        LOG.log(Level.INFO, "{0} not Implemented", op.getOpt());
    }

    static void dontLaunchSummary() {
        SystemDefaults.CLVars.put("dontLaunchSummary", "true");
    }

    static void createStandaloneReport() {
        SystemDefaults.CLVars.put("createStandaloneReport", "true");
    }

    static void reRun() {
        try {
            Control.call();
        } catch (UnCaughtException ex) {
            Logger.getLogger(CLI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void hello() {
        System.out.println("Hello");
    }

    static void executeWith(Map<String, Option> execution) {
        GlobalSettings gSettings = RunManager.getGlobalSettings();
        gSettings.setProjectPath(execution.get(Op.PROJECT_LOC).getValue());
        if (execution.containsKey(Op.RS_NAME)) {
            gSettings.setRelease(execution.get(Op.RS_NAME).getValue());
            gSettings.setTestSet(execution.get(Op.TS_NAME).getValue());
            if (execution.containsKey(Op.BROWSER_NAME)) {
                gSettings.setBrowser(execution.get(Op.BROWSER_NAME).getValue());
            } else {
                gSettings.setBrowser("");
            }
            if(execution.containsKey(Op.TAGS))
              gSettings.setTags(execution.get(Op.TAGS).getValue());
            gSettings.setTestRun(false);
        } else {
            gSettings.setScenario(execution.get(Op.SC_NAME).getValue());
            gSettings.setTestCase(execution.get(Op.TC_NAME).getValue());
            gSettings.setBrowser(execution.get(Op.BROWSER_NAME).getValue());
            gSettings.setTestRun(true);
        }
        if (execution.containsKey(Op.RUN)) {
            try {
                Control.call();
            } catch (UnCaughtException ex) {
                Logger.getLogger(CLI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    static class latest {

        static void getLatestDetails(Map<String, Option> latestOptions) {
            for (Map.Entry<String, Option> entry : latestOptions.entrySet()) {
                String key = entry.getKey();
                Option value = entry.getValue();
                switch (key) {
                    case Op.LATEST_EXE:
                        CLI.latest.exe(value.getValue());
                        break;
                    case Op.LATEST_EXE_LOC:
                        CLI.latest.exe.loc();
                        break;
                    case Op.LATEST_EXE_STATUS:
                        CLI.latest.exe.status();
                        break;
                    case Op.LATEST_EXE_DATA_LOC:
                        CLI.latest.exe.data.loc();
                        break;
                    case Op.LATEST_EXE_DATA_RAW:
                        CLI.latest.exe.data.raw();
                        break;
                    case Op.LATEST_EXE_LOG_LOC:
                        CLI.latest.exe.log.loc();
                        break;
                    case Op.LATEST_EXE_LOG_RAW:
                        CLI.latest.exe.log.raw();
                        break;
                    case Op.LATEST_EXE_PERF_STAT:
                        CLI.latest.exe.perf.validate(value.getValue(), true);
                        break;
                    case Op.LATEST_EXE_PERF_REPORT:
                        CLI.latest.exe.perf.validate(value.getValue(), false);
                        break;
                }
            }
        }

        static void exe(String key) {
            String data = exe.rmJSVar(exe.read(true, latest.exe.data.FN));
            if (data.startsWith("{") && data.endsWith("}")) {
                JSONObject res = (JSONObject) JSONValue.parse(data);
                if (res.containsKey(key)) {
                    System.out.println((String) res.get(key));
                } else {
                    LOG.log(Level.INFO, "ERROR:Key '{0}' not exist", key);
                }
            } else {
                System.out.println(data);
            }

        }

        static class exe {

            static File getLatest() {
                return new File(FilePath.getLatestResultsLocation());
            }

            private static String read(boolean raw, String file) {
                if (isDir(getLatest())) {
                    File data = new File(getLatest(), file);
                    if (isFile(data)) {
                        try {
                            return raw ? FileScanner.readFile(data) : data.getAbsolutePath();
                        } catch (Exception ex) {
                            Logger.getLogger(CLI.class.getName()).log(Level.SEVERE, null, ex);
                            return ex.getMessage();
                        }
                    } else {
                        return "ERROR:no data present";
                    }
                } else {
                    return "ERROR:no result present";
                }
            }

            static class data {

                public static final String FN = "data.js";

                static void loc() {
                    System.out.println(read(false, FN));
                }

                static void raw() {
                    System.out.println(rmJSVar(read(true, FN)));
                }
            }

            static class log {

                public static final String FN = "console.txt";

                static void loc() {
                    System.out.println(read(false, FN));
                }

                static void raw() {
                    System.out.println(read(true, FN));
                }
            }

            static class perf {

                public static final String FN = "perfLog.js";

                static void loc() {
                    System.out.println(read(false, FN));
                }

                static void validate(String arg, boolean statusOnly) {
                    try {
                        Integer tolerance = null;
                        try {
                            if (arg != null) {
                                tolerance = Integer.valueOf(arg);
                            }
                        } catch (Exception ex) {
                        }
                        String data = read(true, FN);
                        // settings.ProjectSettings.setProjectLocation(getProject().getAbsolutePath());
                        JSONObject res = Performance.checkPageLoading(data, tolerance);
                        if (statusOnly) {
                            System.out.println(String.valueOf(result(Boolean.valueOf(res.get("status") + ""))));
                        } else {
                            System.out.println(String.valueOf(res));
                        }
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE, "Error : {0}", ex.getMessage());
                    }
                }
            }

            static void loc() {
                if (isDir(getLatest())) {
                    System.out.println(getLatest().getAbsolutePath());
                } else {
                    System.out.println("ERROR:no result present");
                }
            }

            static void status() {
                String data = rmJSVar(read(true, latest.exe.data.FN));
                if (data.startsWith("{") && data.endsWith("}")) {
                    JSONObject res = (JSONObject) JSONValue.parse(data);
                    int pass = Integer.valueOf(res.get(RDS.TestSet.NO_OF_PASS_TESTS).toString());
                    int fail = Integer.valueOf(res.get(RDS.TestSet.NO_OF_FAIL_TESTS).toString());
                    System.out.println(String.valueOf(result(pass > 0 && fail == 0)));
                } else {
                    System.out.println(data);
                }

            }

            private static boolean isDir(File dir) {
                return dir != null && dir.exists() && dir.isDirectory();
            }

            private static boolean isFile(File f) {
                return f != null && f.exists() && f.isFile();
            }

            private static String rmJSVar(String data) {
                String d = data.replaceFirst("var DATA=", "");
                return d.substring(0, d.length() - 1);
            }
        }
    }

    private static Object result(boolean flag) {
        return flag ? 1 : 0;
    }

    public static class Op {

        public static final String B_DATE = "bDate", B_TIME = "bTime", B_VERSION = "bVersion", HELP = "help",
                TIME = "t", DONT_LAUNCH_SUMMARY = "dont_launch_report", SET_VAR = "setVar", SET_ENV = "setEnv",
                LATEST_EXE = "latest_exe", LATEST_EXE_STATUS = "latest_exe_status", LATEST_EXE_LOC = "latest_exe_loc",
                LATEST_EXE_DATA_LOC = "latest_exe_data_loc", LATEST_EXE_DATA_RAW = "latest_exe_data_raw",
                LATEST_EXE_LOG_LOC = "latest_exe_log_loc", LATEST_EXE_LOG_RAW = "latest_exe_log_raw",
                LATEST_EXE_PERF_STAT = "latest_exe_perf_status", PLOAD_PERF = "checkPagePerf",
                LATEST_EXE_PERF_REPORT = "latest_exe_perf_report", HELLO = "hi";
        public static final String RE_RUN = "rerun";
        public static final String RUN = "run";
        public static final String STANDALONE_REPORT = "standalone_report";
        public static final String PROJECT_LOC = "project_location";
        public static final String TC_NAME = "testcase";
        public static final String SC_NAME = "scenario";
        public static final String RS_NAME = "release";
        public static final String TS_NAME = "testset";
        public static final String TAGS = "tags";
        public static final String BROWSER_NAME = "browser";
        public static final String VERSION = "version";
        public static final String V = "v";
        public static final String DEBUG = "debug";
        public static final String QUIT = "quit";
    }
}
