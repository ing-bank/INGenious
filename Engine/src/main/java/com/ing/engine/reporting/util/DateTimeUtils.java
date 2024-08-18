
package com.ing.engine.reporting.util;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {

    private static final String DATE_FORMAT_NOW = "dd-MMM-yyyy";

    private static final String TIME_FORMAT_NOW = "HH:mm:ss.sss";

    private static final String DATE_FORMAT_FOR_FOLDER = "dd-MMM-yyyy";

    private static final String TIME_FORMAT_FOR_FOLDER = "HH-mm-ss";
    private static final String FORMAT = "%02d:%02d:%02d";
    public final long startTime;

    public DateTimeUtils() {
        startTime = System.currentTimeMillis();
    }

    public static String TimeNow() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }
   

    public static String DateNow() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public static String DateNowForFolder() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_FOR_FOLDER);
        return sdf.format(cal.getTime());
    }

    public static String TimeNowForFolder() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT_FOR_FOLDER);
        return sdf.format(cal.getTime());
    }

    public static String DateTimeNow() {
        return DateTimeUtils.DateNow() + " " + DateTimeUtils.TimeNow();
    }

    public static String DateTimeNowForFolder() {
        return DateTimeUtils.DateNowForFolder() + " " + DateTimeUtils.TimeNowForFolder();
    }

    public String timeRun() {
        final long duration = System.currentTimeMillis() - startTime;
        return parseTime(duration);
    }

    public static String parseTime(long milliseconds) {
        return String.format(FORMAT,
                TimeUnit.MILLISECONDS.toHours(milliseconds),
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(milliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }


}
