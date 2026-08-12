package com.ing.ide.util.logging;

import static com.ing.engine.reporting.impl.ConsoleReport.resetLogger;

import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.reporting.impl.ConsoleReport.MultiOutputStream;
import com.ing.engine.reporting.impl.ConsoleReport.PrintStreamOut;
import com.ing.ide.settings.AppSettings;
import com.ing.ide.util.Utility;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
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
    private JDialog logDialog;
    private JTextArea logArea;

    static {
        com.ing.engine.constants.SystemDefaults.getClassesFromJar.set(true);
        // System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, AppSettings.get("defaultLogLevel"));
        //  System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_DATE_TIME_KEY, AppSettings.get("showDateTime"));
        //  System.setProperty(org.slf4j.impl.SimpleLogger.DATE_TIME_FORMAT_KEY, AppSettings.get("dateTimeFormat"));
        LOG_BKP_LOC = resolveWorkspacePath(AppSettings.get("logBackupLoc"));
        LOG_FILE = resolveWorkspacePath(AppSettings.get("logfile"));
        //   System.setProperty(org.slf4j.impl.SimpleLogger.LEVEL_IN_BRACKETS_KEY, "true");
        //   System.setProperty(org.slf4j.impl.SimpleLogger.SHOW_THREAD_NAME_KEY, "true");
        try {
            maxFileSize = Double.valueOf(AppSettings.get("maxFileSize"));
        } catch (NumberFormatException ex) {
            AppSettings.set("maxlogSize", maxFileSize + "");
            AppSettings.store("Logger properties");
            java
                .util.logging.Logger.getLogger(UILogger.class.getName())
                .log(Level.SEVERE, null, ex);
        }
        UILogger.get().init();
    }

    public static Logger getLogger(String className) {
        return LoggerFactory.getLogger(className);
    }

    private static String resolveWorkspacePath(String configuredPath) {
        File configuredFile = new File(configuredPath);
        if (configuredFile.isAbsolute()) {
            return configuredFile.getAbsolutePath();
        }
        return new File(AppResourcePath.getWorkspaceRoot(), configuredPath).getAbsolutePath();
    }

    private UILogger() {
        try {
            checkFileBackup();
            File logFile = new File(LOG_FILE);
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create log directory: " + parent);
            }
            OutputStream logf = new FileOutputStream(logFile, true);
            MultiOutputStream multiErr = new MultiOutputStream(System.err, logf);
            MultiOutputStream multiOut = new MultiOutputStream(System.out, logf);
            log_err = new PrintStream(multiErr);
            log_out = new PrintStreamOut(multiOut);
            init();
        } catch (Exception ex) {
            reset();
            java
                .util.logging.Logger.getLogger(UILogger.class.getName())
                .log(Level.SEVERE, null, ex);
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
            java
                .util.logging.Logger.getLogger(UILogger.class.getName())
                .log(Level.SEVERE, null, ex);
        }
    }

    public void openLog() {
        showLogDialog();
    }

    private void showLogDialog() {
        if (logDialog == null) {
            logDialog = new JDialog((java.awt.Frame) null, "Show Log", false);
            logDialog.setLayout(new BorderLayout(8, 8));

            logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setLineWrap(false);
            logArea.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(logArea);
            logDialog.add(scrollPane, BorderLayout.CENTER);

            JButton refresh = new JButton("Refresh");
            refresh.addActionListener(e -> loadLogPreview());

            JButton close = new JButton("Close");
            close.addActionListener(e -> logDialog.setVisible(false));

            javax.swing.JPanel controls = new javax.swing.JPanel();
            controls.add(refresh);
            controls.add(close);
            logDialog.add(controls, BorderLayout.SOUTH);

            logDialog
                .getRootPane()
                .registerKeyboardAction(
                    e -> logDialog.setVisible(false),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
                );

            logDialog.setSize(900, 560);
        }

        loadLogPreview();
        logDialog.setLocationRelativeTo(null);
        logDialog.setVisible(true);
        logDialog.toFront();
    }

    private void loadLogPreview() {
        if (logArea == null) {
            return;
        }
        File logFile = new File(LOG_FILE);
        if (!logFile.exists()) {
            logArea.setText("Log file not found: " + LOG_FILE);
            logArea.setCaretPosition(0);
            return;
        }
        try {
            logArea.setText(readLastChars(logFile, 200_000));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } catch (IOException ex) {
            logArea.setText("Unable to load log file: " + ex.getMessage());
            logArea.setCaretPosition(0);
        }
    }

    private String readLastChars(File file, int maxBytes) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            long start = Math.max(0, length - maxBytes);
            raf.seek(start);
            byte[] bytes = new byte[(int) (length - start)];
            raf.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public String getLogFile() {
        return LOG_FILE;
    }
}
