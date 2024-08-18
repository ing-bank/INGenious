
package com.ing.ide.main.dashboard.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class Tools {

    private static final Logger LOG = Logger.getLogger(Tools.class.getName());
    private static final String FORMAR = "dd-MM-yyyy";
    private static final long MILLS_IN_DAY = 1000L * 60 * 60 * 24;

    public static synchronized void writeFile(File f, String s) {
        try (PrintWriter out = new PrintWriter(f)) {
            out.write((s));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public static synchronized String readFile(File path) throws Exception {
        return scanFile(path);

    }

    public static synchronized String scanFile(File path) throws Exception {
        String data = "";
        try (Scanner s = new Scanner(path).useDelimiter("\\A")) {
            if (s.hasNext()) {
                data = s.next();
            }
        }
        return data;
    }

    public static synchronized long getMillisNow(String dateInString) {
        try {
            Date date = getFormatter().parse(dateInString), now = new Date();
            return date.getTime() - now.getTime();
        } catch (java.text.ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public static String today() {
        return getFormatter().format(new Date());
    }

    public static String after(int days) {
        return getFormatter().format(getDateafter(days));
    }

    private static Date getDateafter(int days) {
        return new Date(new Date().getTime() + toMillis(days));
    }

    private static long toMillis(int days) {
        return MILLS_IN_DAY * (long) days;
    }
  

    /**
     * @return the formatter
     */
    public static SimpleDateFormat getFormatter() {
        return new SimpleDateFormat(FORMAR);
    }

    public static Date toDate(String edate) {
        try {
            return getFormatter().parse(edate);
        } catch (ParseException ex) {
            return new Date();
        }
    }

    public static void writeFile(File f, Set<String> set) {
        try (PrintWriter out = new PrintWriter(f)) {
            for (String s : set) {
                out.write(s);
                out.println();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static boolean verifyLocalPort(String server, int port) {
        try {
            new ServerSocket(port).close();
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create " + server + ". Port " + port + " already in use");
        }
    }

    public static Date getScheduledTime() {

        Calendar startTime = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, 0);
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        if (startTime.before(now) || startTime.equals(now)) {
            startTime.add(Calendar.DATE, 1);
        }

        return startTime.getTime();
    }

    public static int toInt(String v, int def) {
        try {
            return Integer.valueOf(v);
        } catch (Exception ex) {
            return def;
        }

    }

    public static String IP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "unable to get ip address.. using 0.0.0.0", ex);
            return "0.0.0.0";
        }
    }

}
