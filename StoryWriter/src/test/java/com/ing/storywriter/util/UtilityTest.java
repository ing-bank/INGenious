package com.ing.storywriter.util;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Date;
import org.testng.annotations.Test;

public class UtilityTest {

    // ---- isEmpty ----

    @Test
    public void testIsEmptyNull() {
        assertThat(Utility.isEmpty(null)).isTrue();
    }

    @Test
    public void testIsEmptyEmptyString() {
        assertThat(Utility.isEmpty("")).isTrue();
    }

    @Test
    public void testIsEmptyWhitespace() {
        assertThat(Utility.isEmpty("   ")).isTrue();
    }

    @Test
    public void testIsEmptyNonEmpty() {
        assertThat(Utility.isEmpty("hello")).isFalse();
    }

    @Test
    public void testIsEmptyNumber() {
        assertThat(Utility.isEmpty(42)).isFalse();
    }

    @Test
    public void testIsEmptyZero() {
        assertThat(Utility.isEmpty(0)).isFalse();
    }

    // ---- getValue ----

    @Test
    public void testGetValueNull() {
        assertThat(Utility.getValue(null)).isEmpty();
    }

    @Test
    public void testGetValueEmpty() {
        assertThat(Utility.getValue("")).isEmpty();
    }

    @Test
    public void testGetValueNonEmpty() {
        assertThat(Utility.getValue("test")).isEqualTo("test");
    }

    @Test
    public void testGetValueNumber() {
        assertThat(Utility.getValue(123)).isEqualTo("123");
    }

    @Test
    public void testGetValueWhitespace() {
        assertThat(Utility.getValue("  ")).isEmpty();
    }

    // ---- getDays ----

    @Test
    public void testGetDaysNullReturns30() {
        assertThat(Utility.getDays(null)).isEqualTo(30);
    }

    @Test
    public void testGetDaysFutureDatePositive() {
        Date future = new Date(System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000);
        int days = Utility.getDays(future);
        assertThat(days).isBetween(9, 10);
    }

    @Test
    public void testGetDaysPastDateNegative() {
        Date past = new Date(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000);
        int days = Utility.getDays(past);
        assertThat(days).isBetween(-6, -4);
    }

    @Test
    public void testGetDaysTodayZero() {
        Date today = new Date();
        int days = Utility.getDays(today);
        assertThat(days).isBetween(-1, 0);
    }

    // ---- getdatetimeString / getdateString ----

    @Test
    public void testGetDateTimeStringFormat() {
        String dt = Utility.getdatetimeString();
        assertThat(dt).isNotNull();
        assertThat(dt).contains("_");
        // Format: MM-dd-yyyy_hh-mm-ssAM/PM
        assertThat(dt).matches("\\d{2}-\\d{2}-\\d{4}_\\d{2}-\\d{2}-\\d{2}\\s?[aApP][mM]");
    }

    @Test
    public void testGetDateStringFormat() {
        String d = Utility.getdateString();
        assertThat(d).matches("\\d{2}-\\d{2}-\\d{4}");
    }
}
