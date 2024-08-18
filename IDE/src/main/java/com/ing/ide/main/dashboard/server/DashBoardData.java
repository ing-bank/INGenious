
package com.ing.ide.main.dashboard.server;

import java.io.File;

/**
 *
 * 
 */
public class DashBoardData {

    public static final File REPORT = new File("web");
    public static final File DASH_BOARD = new File(REPORT, "dashboard");
    public static final File HAR_COMPARE = new File(DASH_BOARD, "harCompare");

    private static String projectLocation;

    public static void setProjLocation(String projectLocation) {
        DashBoardData.projectLocation = projectLocation;
    }

    public static String project() {
        return projectLocation;
    }

    public static File report() {
        return REPORT;
    }

    public static File dashBoard() {
        return DASH_BOARD;
    }

    public static File harCompare() {
        return HAR_COMPARE;
    }

    public static File results() {
        return new File(project(), "Results");
    }

    public static File perf() {
        return new File(results(), "perf");
    }

    public static File hars() {
        return new File(perf(), "har");
    }

    public static File refHars() {
        return new File(perf(), "refhar");
    }

    public static File config() {
        return new File(perf(), "config.json");
    }
}
