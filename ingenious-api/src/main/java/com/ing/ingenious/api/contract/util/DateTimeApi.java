package com.ing.ingenious.api.contract.util;

/**
 * Contract for date/time formatting utilities available to plugins.
 * <p>
 * Implementations back these methods with INGenious's own timestamp
 * formatting so that reports, logs, and folder names written by a plugin
 * stay consistent with those produced by the framework itself.
 * </p>
 * <p>
 * <b>Locale note:</b> the day-month-year formats below use a three-letter
 * month abbreviation ({@code MMM}) rendered using the host JVM's default
 * locale. On a non-English JVM this may not produce English month names.
 * </p>
 */
public interface DateTimeApi {

    /**
     * Returns the current date formatted as {@code dd-MMM-yyyy} (e.g. {@code 15-Oct-2026}).
     * @return the formatted current date
     */
    String dateNow();

    /**
     * Returns the current time formatted as {@code HH:mm:ss.SSS} (e.g. {@code 05:33:41.123}).
     * @return the formatted current time
     */
    String timeNow();

    /**
     * Returns the current date and time formatted as {@code dateNow() + " " + timeNow()}.
     * @return the formatted current date and time
     */
    String dateTimeNow();

    /**
     * Returns the current date formatted as {@code dd-MMM-yyyy}, for use in file/folder names.
     * <p>As of the current implementation this uses the same format as {@link #dateNow()}.</p>
     * @return the formatted current date
     */
    String dateNowForFolder();

    /**
     * Returns the current time formatted as {@code HH-mm-ss} (hyphen-separated, filesystem-safe),
     * for use in file/folder names.
     * @return the formatted current time
     */
    String timeNowForFolder();

    /**
     * Returns the current date and time formatted as
     * {@code dateNowForFolder() + " " + timeNowForFolder()}, for use in file/folder names.
     * <p>Note the result contains a space character.</p>
     * @return the formatted current date and time
     */
    String dateTimeNowForFolder();

    /**
     * Formats a duration in milliseconds as {@code HH:mm:ss}.
     * @param milliseconds the duration in milliseconds
     * @return the formatted duration
     */
    String parseTime(long milliseconds);
}
