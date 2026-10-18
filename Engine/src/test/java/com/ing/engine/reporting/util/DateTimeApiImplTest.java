package com.ing.engine.reporting.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Tests for {@link DateTimeApiImpl} — verifies each method delegates to the
 * corresponding {@link DateTimeUtils} static and that formats match the
 * documented {@link com.ing.ingenious.api.contract.util.DateTimeApi} contract.
 * Format assertions use regex rather than exact string comparisons against
 * {@link DateTimeUtils} to avoid flakiness around clock-tick boundaries.
 */
public class DateTimeApiImplTest {
    private final DateTimeApiImpl dateTimeApi = DateTimeApiImpl.INSTANCE;

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(DateTimeApiImpl.INSTANCE).isNotNull();
        assertThat(DateTimeApiImpl.INSTANCE).isSameAs(DateTimeApiImpl.INSTANCE);
    }

    @Test
    public void dateNowShouldMatchDateTimeUtilsFormat() {
        assertThat(dateTimeApi.dateNow()).matches("\\d{2}-[A-Za-z]{3}-\\d{4}");
    }

    @Test
    public void timeNowShouldMatchDateTimeUtilsFormat() {
        assertThat(dateTimeApi.timeNow()).matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
    }

    @Test
    public void dateTimeNowShouldCombineDateAndTime() {
        assertThat(dateTimeApi.dateTimeNow())
            .matches("\\d{2}-[A-Za-z]{3}-\\d{4} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
    }

    @Test
    public void dateNowForFolderShouldMatchDateTimeUtilsFormat() {
        assertThat(dateTimeApi.dateNowForFolder()).matches("\\d{2}-[A-Za-z]{3}-\\d{4}");
    }

    @Test
    public void timeNowForFolderShouldBeFilesystemSafe() {
        assertThat(dateTimeApi.timeNowForFolder()).matches("\\d{2}-\\d{2}-\\d{2}");
    }

    @Test
    public void dateTimeNowForFolderShouldCombineDateAndTimeWithSpace() {
        assertThat(dateTimeApi.dateTimeNowForFolder())
            .matches("\\d{2}-[A-Za-z]{3}-\\d{4} \\d{2}-\\d{2}-\\d{2}");
    }

    @Test
    public void parseTimeShouldDelegateToDateTimeUtils() {
        long durationMillis = 3_723_000L; // 1h 2m 3s
        assertThat(dateTimeApi.parseTime(durationMillis))
            .isEqualTo(DateTimeUtils.parseTime(durationMillis));
    }
}
