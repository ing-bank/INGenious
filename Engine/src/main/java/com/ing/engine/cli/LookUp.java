
package com.ing.engine.cli;

import com.ing.engine.cli.CLI.Op;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 *
 */
public class LookUp {

    private static final Logger LOG = Logger.getLogger(LookUp.class.getName());
    public static boolean cliflag= false;
    public static final Options OPTIONS = new Options();
    private static final Map<String, Option> DO_LATER = new HashMap<>();
    private static final Map<String, Option> EXECUTION = new HashMap<>();

    private static final Map<String, Option> LATEST = new HashMap<>();

    static {
        OPTIONS.addOption(Op.V, Op.VERSION, false, "Display current build details");

        OPTIONS.addOption(Op.RUN, false, "Run with the given details");
        OPTIONS.addOption(Op.RE_RUN, false, "Rerun the last execution");

        OPTIONS.addOption(Op.PROJECT_LOC, true, "Project Location for Execution");
        OPTIONS.addOption(Op.SC_NAME, true, "Scenario Name");
        OPTIONS.addOption(Op.TC_NAME, true, "Testcase Name");
        OPTIONS.addOption(Op.BROWSER_NAME, true, "Browser Name (Not applicable for Testset Execution)");
        OPTIONS.addOption(Op.RS_NAME, true, "Release Name");
        OPTIONS.addOption(Op.TS_NAME, true, "Testset Name");
        OPTIONS.addOption(Op.TAGS, true, "Tags");
        OPTIONS.addOption(Op.B_DATE, false, "Display current build date");
        OPTIONS.addOption(Op.B_TIME, false, "Display current build time");
        OPTIONS.addOption(Op.B_VERSION, false, "Display current build version");
        OPTIONS.addOption(Op.DONT_LAUNCH_SUMMARY, false, "Disables launching summary report after execution");
        OPTIONS.addOption(Op.DEBUG, false, "Enable debug mode");
        OPTIONS.addOption(Op.HELP, false, "Help");
        OPTIONS.addOption(Op.HELLO, false, "Says Hello!");
        OPTIONS.addOption(Op.TIME, false, "Display Current Time");
        OPTIONS.addOption(Op.LATEST_EXE, true, "Returns the given property value for the latest execution");
        OPTIONS.addOption(Op.LATEST_EXE_LOC, false, "Returns the results folder for the latest execution");
        OPTIONS.addOption(Op.LATEST_EXE_STATUS, false, "Returns the status for the latest execution");
        OPTIONS.addOption(Op.LATEST_EXE_DATA_LOC, false, "Returns the Report data location for the latest execution");
        OPTIONS.addOption(Op.LATEST_EXE_DATA_RAW, false, "Returns the Report data for the latest execution");
        OPTIONS.addOption(Op.LATEST_EXE_LOG_LOC, false, "Returns the log file location for the latest execution");
        OPTIONS.addOption(Op.LATEST_EXE_LOG_RAW, false, "Returns the log file for the latest execution");
        OPTIONS.addOption(Op.LATEST_EXE_LOG_RAW, false, "Returns the log file for the latest execution");

        OPTIONS.addOption(Op.LATEST_EXE_PERF_STAT, true, "Returns the page load performance results for latest execution");
        OPTIONS.getOption(Op.LATEST_EXE_PERF_STAT).setOptionalArg(true);
        OPTIONS.addOption(Op.LATEST_EXE_PERF_REPORT, true, "Returns the page load performance report for latest execution");
        OPTIONS.getOption(Op.LATEST_EXE_PERF_REPORT).setOptionalArg(true);

        OPTIONS.addOption(Op.PLOAD_PERF, true, "Returns the page load performance results after Run");
        OPTIONS.getOption(Op.PLOAD_PERF).setOptionalArg(true);

        OPTIONS.addOption(Op.SET_VAR, true, "Create/Set user defined variable [-setVar \"var=value\"]");
        OPTIONS.addOption(Op.SET_ENV, true, "Create/Set Env settings(override) from Command Line.\n"
                + "Global settings (exe), Run Settings (run), User Defined Settings (user), Driver Settings (driver), Test Management Settings (tm)\n"
                + "[-setEnv \"run.var=value;exe.var=value;user.var=value\"]");
        OPTIONS.addOption(Op.TIME, false, "Display Current Time");

        OPTIONS.addOption(Op.STANDALONE_REPORT, false, "Create Standalone Report instead of Relative one");
        OPTIONS.addOption(Op.QUIT,false, "Quit after execution is complete");
        DO_LATER.clear();
        EXECUTION.clear();
    }

    public static void exe(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            cliflag = true;
            CommandLine cmd = parser.parse(OPTIONS, args);
            for (Option op : cmd.getOptions()) {

                switch (op.getOpt()) {
                    case Op.B_DATE:
                    case Op.B_TIME:
                    case Op.B_VERSION:
                    case Op.V:
                    case Op.VERSION:
                        break;
                    case Op.TIME:
                        CLI.t();
                        break;
                    case Op.HELP:
                        CLI.help();
                        break;
                    case Op.DONT_LAUNCH_SUMMARY:
                        CLI.dontLaunchSummary();
                        break;
                    case Op.STANDALONE_REPORT:
                        CLI.createStandaloneReport();
                        break;
                    case Op.DEBUG:
                        /*
                        * set debug=true
                         */
                        CLI.setVar(Op.DEBUG);
                        break;
                    case Op.SET_VAR:
                        CLI.setVar(op.getValue());
                        break;
                    case Op.SET_ENV:
                        CLI.setEnv(op.getValue());
                        break;
                    case Op.LATEST_EXE:
                    case Op.LATEST_EXE_LOC:
                    case Op.LATEST_EXE_STATUS:
                    case Op.LATEST_EXE_DATA_LOC:
                    case Op.LATEST_EXE_DATA_RAW:
                    case Op.LATEST_EXE_LOG_LOC:
                    case Op.LATEST_EXE_LOG_RAW:
                    case Op.LATEST_EXE_PERF_STAT:
                    case Op.LATEST_EXE_PERF_REPORT:
                        LATEST.put(op.getOpt(), op);
                        break;
                    case Op.RE_RUN:
                        DO_LATER.put(op.getOpt(), op);
                        break;
                    case Op.PLOAD_PERF:
                        DO_LATER.put(op.getOpt(), op);
                        break;
                    case Op.HELLO:
                        CLI.hello();
                        break;
                    case Op.RUN:
                    case Op.PROJECT_LOC:
                    case Op.TC_NAME:
                    case Op.TS_NAME:
                    case Op.SC_NAME:
                    case Op.RS_NAME:
                    case Op.BROWSER_NAME:
                    case Op.TAGS:
                        EXECUTION.put(op.getOpt(), op);
                        break;
                    default:
                        CLI.Default(op);
                        break;
                }
            }
            if (DO_LATER.containsKey(Op.RE_RUN)) {
                CLI.reRun();
                if (DO_LATER.containsKey(Op.PLOAD_PERF)) {
                    CLI.latest.exe.perf.validate(DO_LATER.get(Op.PLOAD_PERF).getValue(), true);
                }
            } else if (!EXECUTION.isEmpty()) {
                CLI.executeWith(EXECUTION);
                if (!EXECUTION.containsKey(Op.RUN) && !LATEST.isEmpty()) {
                    CLI.latest.getLatestDetails(LATEST);
                }

            }

        } catch (ParseException ex) {
            LOG.severe(ex.getMessage());
        } catch (Exception ex) {
            Logger.getLogger(LookUp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
