package com.ing.engine.reporting.util;

import com.ing.ingenious.api.contract.util.DateTimeApi;

/**
 * Delegates the {@link DateTimeApi} plugin contract to the existing
 * {@link DateTimeUtils} static methods, so plugins and the framework's own
 * report handlers always produce identically formatted timestamps.
 */
public final class DateTimeApiImpl implements DateTimeApi {
    public static final DateTimeApiImpl INSTANCE = new DateTimeApiImpl();

    private DateTimeApiImpl() {}

    @Override
    public String dateNow() {
        return DateTimeUtils.DateNow();
    }

    @Override
    public String timeNow() {
        return DateTimeUtils.TimeNow();
    }

    @Override
    public String dateTimeNow() {
        return DateTimeUtils.DateTimeNow();
    }

    @Override
    public String dateNowForFolder() {
        return DateTimeUtils.DateNowForFolder();
    }

    @Override
    public String timeNowForFolder() {
        return DateTimeUtils.TimeNowForFolder();
    }

    @Override
    public String dateTimeNowForFolder() {
        return DateTimeUtils.DateTimeNowForFolder();
    }

    @Override
    public String parseTime(long milliseconds) {
        return DateTimeUtils.parseTime(milliseconds);
    }
}
