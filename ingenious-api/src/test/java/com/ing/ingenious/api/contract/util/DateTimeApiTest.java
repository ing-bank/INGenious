package com.ing.ingenious.api.contract.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Contract test for {@link DateTimeApi}. Uses a minimal in-test implementation
 * to verify the interface can be implemented and dispatched without pulling
 * in any Engine dependency.
 */
class DateTimeApiTest {

    private final DateTimeApi dateTimeApi = new DateTimeApi() {
        @Override
        public String dateNow() {
            return "15-Oct-2026";
        }

        @Override
        public String timeNow() {
            return "05:33:41.123";
        }

        @Override
        public String dateTimeNow() {
            return dateNow() + " " + timeNow();
        }

        @Override
        public String dateNowForFolder() {
            return dateNow();
        }

        @Override
        public String timeNowForFolder() {
            return "05-33-41";
        }

        @Override
        public String dateTimeNowForFolder() {
            return dateNowForFolder() + " " + timeNowForFolder();
        }

        @Override
        public String parseTime(long milliseconds) {
            return "00:00:01";
        }
    };

    @Test
    void dateAndTimeMethodsShouldReturnFormattedStrings() {
        assertEquals("15-Oct-2026", dateTimeApi.dateNow());
        assertEquals("05:33:41.123", dateTimeApi.timeNow());
        assertEquals("15-Oct-2026 05:33:41.123", dateTimeApi.dateTimeNow());
    }

    @Test
    void folderVariantsShouldReturnFilesystemSafeStrings() {
        assertEquals("15-Oct-2026", dateTimeApi.dateNowForFolder());
        assertEquals("05-33-41", dateTimeApi.timeNowForFolder());
        assertEquals("15-Oct-2026 05-33-41", dateTimeApi.dateTimeNowForFolder());
    }

    @Test
    void parseTimeShouldReturnNonNullDuration() {
        assertNotNull(dateTimeApi.parseTime(1000L));
    }
}
