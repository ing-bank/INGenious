
package com.ing.ide.util.logging;

import com.ing.engine.reporting.impl.ConsoleReport.MultiOutputStream;
import com.ing.engine.reporting.impl.ConsoleReport.PrintStreamOut;
import static com.ing.engine.reporting.impl.ConsoleReport.resetLogger;
import com.ing.engine.support.DesktopApi;
import com.ing.ide.settings.AppSettings;
import com.ing.ide.util.Utility;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 
 */
public final class UILogger {

    public static PrintStream log_out, log_err, logStream, sysout, syserr;
    private static final PrintStream SYS_OUT = System.out, SYS_ERR = System.err;
    private static UILogger logger;
    private static final String LOG_FILE;
    private static double maxFileSize = 4.5d;
    private static final String LOG_BKP_LOC;

    static {
        com.ing.engine.constants.SystemDefaults.getClassesFromJar.set(true);
       // System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, AppSettings.get("defaultLogLevel"));
      //  System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_DATE_TIME_KEY, AppSettings.get("showDateTime"));
      //  System.setProperty(org.slf4j.impl.SimpleLogger.DATE_TIME_FORMAT_KEY, AppSettings.get("dateTimeFormat"));
        LOG_BKP_LOC = AppSettings.get("logBackupLoc");
        LOG_FILE = AppSettings.get("logfile");
     //   System.setProperty(org.slf4j.impl.SimpleLogger.LEVEL_IN_BRACKETS_KEY, "true");
     //   System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_THREAD_NAME_KEY, "true");
        try {
            maxFileSize = Double.valueOf(AppSettings.get("maxFileSize"));
        } catch (NumberFormatException ex) {
            AppSettings.set("maxlogSize", maxFileSize + "");
            AppSettings.store("Logger properties");
            java.util.logging.Logger.getLogger(UILogger.class.getName()).log(Level.SEVERE, null, ex);
        }
        UILogger.get().init();
    }

    public static Logger getLogger(String className) {
        return LoggerFactory.getLogger(className);
    }

    private UILogger() {
        try {
            checkFileBackup();
            OutputStream logf = new FileOutputStream(LOG_FILE, true);
            MultiOutputStream multiErr = new MultiOutputStream(System.err, logf);
            MultiOutputStream multiOut = new MultiOutputStream(System.out, logf);
            log_err = new PrintStream(multiErr);
            log_out = new PrintStreamOut(multiOut);
            init();
        } catch (Exception ex) {
            reset();
            java.util.logging.Logger.getLogger(UILogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * returns the logger object.
     *
     * @return
     */
    public static UILogger get() {
        if (logger == null) {
            logger = new UILogger();
        }
        return logger;
    }

    public void revertToDefault() {
        init();
    }

    /**
     * sets up the log stream
     */
    public void init() {
        System.setErr(log_err);
        System.setOut(log_out);
        resetLogger();

    }

    public static void reset() {
        System.setOut(SYS_OUT);
        System.setErr(SYS_ERR);
        resetLogger();
    }

    private void checkFileBackup() {
        File log = new File(LOG_FILE);
        if (log.exists() && log.isFile()) {
            double bytes = log.length();
            double mb = bytes / (1024 * 1024);
            if (mb > maxFileSize) {
                backupLog();
            }
        }
    }

    private void backupLog() {
        try {
            File bkp = new File(LOG_BKP_LOC);
            if (!bkp.exists()) {
                bkp.mkdirs();
            }
            String filename = "log-" + Utility.getdatetimeString() + ".txt";
            FileUtils.moveFile(new File(LOG_FILE), new File(bkp, filename));
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(UILogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void openLog() {
       DesktopApi.open(new File(LOG_FILE));
    }

    String getLogFile() {
        return LOG_FILE;
    }
}
