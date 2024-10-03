package com.ing.engine.reporting.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 *
 */
@SuppressWarnings("unchecked")
public class RDS {

    private final static String BEFORE = "var DATA=", AFTER = ";";

    public synchronized static JSONObject getNewStep(String name) {
        JSONObject step = new JSONObject();
        step.put(Step.TYPE, "step");
        step.put(Step.NAME, name == null ? "Description Not Given" : name);
        step.put(Step.DATA, new JSONObject());
        return step;
    }

    public synchronized static JSONObject getNewIteration(String name) {
        JSONObject iteration = new JSONObject();
        iteration.put(Step.TYPE, "iteration");
        iteration.put(Step.NAME, name);
        iteration.put(Step.DATA, new JSONArray());
        iteration.put(TestCase.STATUS, "");
        return iteration;
    }

    public synchronized static JSONObject getNewReusable(String name, String desc) {
        JSONObject reusable = new JSONObject();
        reusable.put(Step.TYPE, "reusable");
        reusable.put(Step.NAME, name);
        reusable.put(Step.DESCRIPTION, desc);
        reusable.put(Step.DATA, new JSONArray());
        reusable.put(TestCase.STATUS, "");
        reusable.put(Step.START_TIME, DateTimeUtils.DateTimeNow());
        return reusable;
    }

    public synchronized static void writeToDataJS(String fileToWrite, JSONObject data) {
        writeToFile(fileToWrite, BEFORE + data.toString() + AFTER);
    }

    public synchronized static void writeToFile(String fileToWrite, String data) {
        try (BufferedWriter bufwriter = new BufferedWriter(new FileWriter(fileToWrite))) {
            bufwriter.write(data);
        } catch (IOException ex) {
            Logger.getLogger(RDS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public class TestSet {

        public static final String PROJECT_NAME = "projectName";
        public static final String RELEASE_NAME = "releaseName";
        public static final String TESTSET_NAME = "testsetName";
        public static final String ITERATION_MODE = "iterationMode";
        public static final String RUN_CONFIG = "runConfiguration";
        public static final String MAX_THREADS = "maxThreads";
        public static final String START_TIME = "startTime";
        public static final String END_TIME = "endTime";
        public static final String EXE_TIME = "exeTime";
        public static final String NO_OF_TESTS = "noTests";
        public static final String NO_OF_PASS_TESTS = "nopassTests";
        public static final String NO_OF_FAIL_TESTS = "nofailTests";
        public static final String THEME = "theme";
        public static final String THEMES = "themes";
        public static final String TEST_RUN = "testRun";
        public static final String EXECUTIONS = "EXECUTIONS";

        public static final String BDD_STYLE = "bddReport";
        public static final String AXE_REPORT = "axeReport";
        public static final String PERF_REPORT = "perfReport";
        public static final String VIDEO_REPORT = "videoReport";
        public static final String VIDEO_REPORT_DIR = "videoReportDir";
    }

    public class TestCase {

        public static final String SCENARIO_NAME = "scenarioName";
        public static final String TESTCASE_NAME = "testcaseName";
        public static final String DESCRIPTION = "description";
        public static final String ITERATIONS = "iterations";
        public static final String ITERATION_TYPE = "iterationType";
        public static final String PLATFORM = "platform";
        public static final String B_VERSION = "bversion";
        public static final String START_TIME = "startTime";
        public static final String END_TIME = "endTime";
        public static final String EXE_TIME = "exeTime";
        public static final String NO_OF_TESTS = "noTests";
        public static final String NO_OF_PASS_TESTS = "nopassTests";
        public static final String NO_OF_FAIL_TESTS = "nofailTests";
        public static final String BROWSER = "browser";
        public static final String STATUS = "status";
        public static final String STEPS = "STEPS";

    }

    public class Step {

        public static final String NAME = "name";
        public static final String TYPE = "type";
        public static final String DATA = "data";
        public static final String DESCRIPTION = "description";
        public static final String START_TIME = "startTime";
        public static final String END_TIME = "endTime";

        public class Data {

            public static final String STEP_NO = "stepno";
            public static final String STEP_NAME = "stepName";
            public static final String ACTION = "action";
            public static final String DESCRIPTION = "description";
            public static final String STATUS = "status";
            public static final String TIME_STAMP = "tStamp";
            public static final String LINK = "link";
            public static final String EXPECTED = "expected";
            public static final String ACTUAL = "actual";
            public static final String COMPARISION = "comparison";
            public static final String OBJECTS = "objects";
        }
    }

}
