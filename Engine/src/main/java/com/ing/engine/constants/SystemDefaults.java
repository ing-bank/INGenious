
package com.ing.engine.constants;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemDefaults {

    public static Duration waitTime = Duration.ofSeconds(10);
    public static Duration elementWaitTime = Duration.ofSeconds(10);
    public static AtomicBoolean stopExecution = new AtomicBoolean();
    public static AtomicBoolean pauseExecution = new AtomicBoolean(false);
    public static AtomicBoolean debugMode = new AtomicBoolean();
    public static AtomicBoolean stopCurrentIteration = new AtomicBoolean();
    public static AtomicBoolean getClassesFromJar = new AtomicBoolean();
    public static AtomicBoolean reportComplete = new AtomicBoolean();
    public static AtomicBoolean nextStepflag = new AtomicBoolean(true);
    public static Map<String, String> CLVars = new HashMap<>();
    public static Map<String, String> EnvVars = new HashMap<>();

    private static Properties buildProperties;

    static {
        buildProperties = new Properties();
        try {
            buildProperties.load(SystemDefaults.class.getResourceAsStream("/engine/build.properties"));
        } catch (IOException ex) {
            Logger.getLogger(SystemDefaults.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static String getBuildVersion() {
        return buildProperties.getProperty("Bundle-Version");
    }

    public static void pollWait() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SystemDefaults.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void resetAll() {
        waitTime = Duration.ofSeconds(10);
        elementWaitTime =  Duration.ofSeconds(10);
        stopExecution = new AtomicBoolean();
        debugMode = new AtomicBoolean();
        stopCurrentIteration = new AtomicBoolean();
        reportComplete = new AtomicBoolean();
        nextStepflag = new AtomicBoolean(true);
        pauseExecution = new AtomicBoolean(false);
    }

    public static boolean canLaunchSummary() {
        return !CLVars.containsKey("dontLaunchSummary");
    }

    public static void printSystemInfo() {
        System.out.println("Run Information");
        System.out.println("========================");
        System.out.println("INGenious Playwright Studio engine : " + getBuildVersion());
        printSystemInfo("java.runtime.name");
        printSystemInfo("java.version");
        printSystemInfo("java.home");
        printSystemInfo("os.name");
        printSystemInfo("os.arch");
        printSystemInfo("os.version");
        printSystemInfo("file.encoding");
        System.out.println("========================");
    }

    private static void printSystemInfo(String key) {
        System.out.println(String.format("%-20s : %s", key, System.getProperty(key)));
    }

    public static boolean debug() {
        return debugMode.get() || Boolean.valueOf(CLVars.get("debug"));
    }
}
