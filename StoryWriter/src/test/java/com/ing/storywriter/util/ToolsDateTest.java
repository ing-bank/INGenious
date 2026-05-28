package com.ing.storywriter.util;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.testng.annotations.Test;

/**
 * Tests for date-oriented utility methods in Tools.
 * File I/O methods are excluded (need temp files and are side-effect heavy).
 */
public class ToolsDateTest {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

    @Test
    public void testTodayFormat() {
        String today = Tools.today();
        assertThat(today).matches("\\d{2}-\\d{2}-\\d{4}");
    }

    @Test
    public void testTodayMatchesNow() {
        String expected = formatter.format(new Date());
        assertThat(Tools.today()).isEqualTo(expected);
    }

    @Test
    public void testAfterPositiveDays() {
        String result = Tools.after(1);
        assertThat(result).matches("\\d{2}-\\d{2}-\\d{4}");
        // Tomorrow should differ from today (except edge case at midnight)
    }

    @Test
    public void testAfterZeroDays() {
        String result = Tools.after(0);
        // Same day
        assertThat(result).isEqualTo(Tools.today());
    }

    @Test
    public void testToDateValid() {
        Date d = Tools.toDate("15-06-2024");
        assertThat(d).isNotNull();
        String formatted = formatter.format(d);
        assertThat(formatted).isEqualTo("15-06-2024");
    }

    @Test
    public void testToDateInvalidReturnsCurrent() {
        Date d = Tools.toDate("not-a-date");
        // Should return new Date() on parse failure
        assertThat(d).isNotNull();
    }

    @Test
    public void testGetMillisNowValidDate() {
        // A future date should give positive millis
        String futureDate = Tools.after(30);
        long millis = Tools.getMillisNow(futureDate);
        assertThat(millis).isGreaterThan(0);
    }

    @Test
    public void testGetMillisNowInvalidDate() {
        long millis = Tools.getMillisNow("invalid");
        assertThat(millis).isEqualTo(-1);
    }
}
