
package com.ing.engine.reporting.impl;

import com.ing.engine.constants.FilePath;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class ConsoleReport {

    private static PrintStream originalOut, originalErr;
    private static FileOutputStream fout, ferr;

    public static File consoleFile;

    public static void init() {
        try {
            File results = new File(FilePath.getCurrentResultsPath());
            consoleFile = new File(results, "console.txt");
            results.mkdirs();
            consoleFile.createNewFile();

            fout = new FileOutputStream(consoleFile, true);
            ferr = new FileOutputStream(consoleFile, true);

            originalOut = System.out;
            originalErr = System.err;

            MultiOutputStream multiOut = new MultiOutputStream(fout, System.out);
            MultiOutputStream multiErr = new MultiOutputStream(ferr, System.err);

            PrintStream stdout = new PrintStreamOut(multiOut);
            PrintStream stderr = new PrintStream(multiErr);

            System.setOut(stdout);
            System.setErr(stderr);
            resetLogger();
        } catch (Exception ex) {
            Logger.getLogger(ConsoleReport.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void reset() {
        Logger.getLogger(ConsoleReport.class.getName()).log(Level.INFO, "Resetting.");

        System.setOut(originalOut);
        System.setErr(originalErr);

        Logger.getLogger(ConsoleReport.class.getName()).log(Level.INFO, "Reset system elements.");
        try {
            Logger.getLogger(ConsoleReport.class.getName()).log(Level.INFO, "Closing file output streams.");
            fout.close();
            Logger.getLogger(ConsoleReport.class.getName()).log(Level.INFO, "Closing file error streams.");
            ferr.close();
            Logger.getLogger(ConsoleReport.class.getName()).log(Level.INFO, "Streams closed.");
        } catch (IOException ex) {
            Logger.getLogger(ConsoleReport.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        Logger.getLogger(ConsoleReport.class.getName()).log(Level.INFO, "File output streams closed.");
        resetLogger();
        Logger.getLogger(ConsoleReport.class.getName()).log(Level.INFO, "Logger was reset.");
    }

    public static void resetLogger() {
        Logger rootLogger = Logger.getLogger("");
        Handler consoleHandler = null;
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                consoleHandler = handler;
                break;
            }
        }
        if (consoleHandler != null) {
            rootLogger.removeHandler(consoleHandler);
        }
        consoleHandler = new ConsoleHandler();
        rootLogger.addHandler(consoleHandler);
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFilter(new Filter() {
            @Override
            public boolean isLoggable(LogRecord lr) {
                return !Objects.toString(lr.getMessage(), "").startsWith("Augmenter should be");
            }
        });
    }

    public static class PrintStreamOut extends PrintStream {

        public PrintStreamOut(OutputStream out) {
            super(out);
        }

        @Override
        public void println(String a) {
            super.println(a);
        }

    }

    public static class MultiOutputStream extends OutputStream {

        OutputStream[] outputStreams;

        public MultiOutputStream(OutputStream... outputStreams) {
            this.outputStreams = outputStreams;
        }

        @Override
        public void write(int b) throws IOException {
            for (OutputStream out : outputStreams) {
                out.write(b);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            for (OutputStream out : outputStreams) {
                out.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (OutputStream out : outputStreams) {
                out.write(b, off, len);
            }
        }

        @Override
        public void flush() throws IOException {
            for (OutputStream out : outputStreams) {
                out.flush();
            }
        }

        @Override
        public void close() throws IOException {
            for (OutputStream out : outputStreams) {
                out.close();
            }
        }
    }
}
