
package com.ing.storywriter.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 *
 */
public class FileLogger {

    static PrintStream fileStream, consoleStream, logStream;
    private static FileLogger logger;
    private static String logfile;
    private static double maxFileSize;
    private static String logBackupLoc;
    final static SimpleDateFormat Datefileformat = new SimpleDateFormat("MM-dd-yyyy"),
            Timefileformat = new SimpleDateFormat("hh-mm-ssa");

    private FileLogger() {
        try {
            maxFileSize = 4.5d;
            logBackupLoc = "bkp";
            logfile = "StoryMaker.log.txt";
            consoleStream = System.out;
            checkFileBackup();
            OutputStream logf = new FileOutputStream(logfile, true);
            fileStream = new PrintStream(logf, true) {
                @Override
                public void println(String x) {
                    consoleStream.println(x);
                    x = ("@" + new Date().toString() + ": ") + x;
                    super.println(x);
                }
            };
            // fileStream = consoleStream;//for dev debugg
            logStream = fileStream;

        } catch (Exception ex) {
            logStream = consoleStream;
        }
    }

    /**
     * returns the logger object.
     *
     * @return
     */
    public static FileLogger get() {
        if (logger == null) {
            logger = new FileLogger();
        }
        return logger;
    }

    /**
     * sets up the log stream
     */
    public void init() {
        System.setOut(getStream());
        System.setErr(getStream());
    }

    /**
     * returns the current log stream
     *
     * @return the current log stream
     */
    public PrintStream getStream() {
        return logStream;
    }

    /**
     * sets the given stream for logging
     *
     * @param stream - stream to set
     */
    public void setStream(PrintStream stream) {
        System.setOut(stream);
        System.setErr(stream);

    }

    /**
     * refresh or reset the log stream
     */
    void setStream() {
        init();
    }

    private void checkFileBackup() {
        File log = new File(logfile);
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
            File bkp = new File(logBackupLoc);
            if (!bkp.exists()) {
                bkp.mkdirs();
            }
            String filename = "log-" + getdatetimeString() + ".txt";
            Files.move(new File(logfile).toPath(), new File(bkp, filename).toPath());
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FileLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getdatetimeString() {
        Date dat = new Date();
        return Datefileformat.format(dat) + "_" + Timefileformat.format(dat);
    }
}
