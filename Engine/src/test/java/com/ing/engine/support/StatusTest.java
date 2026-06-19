package com.ing.engine.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ingenious.api.status.Status;
import org.testng.annotations.Test;

public class StatusTest {

    // ---- toString mapping ----

    @Test
    public void testDoneToString() {
        assertThat(Status.DONE.toString()).isEqualTo("DONE");
    }

    @Test
    public void testPassToString() {
        assertThat(Status.PASS.toString()).isEqualTo("PASS");
    }

    @Test
    public void testPassNsToString() {
        assertThat(Status.PASSNS.toString()).isEqualTo("PASS");
    }

    @Test
    public void testFailToString() {
        assertThat(Status.FAIL.toString()).isEqualTo("FAIL");
    }

    @Test
    public void testFailNsToString() {
        assertThat(Status.FAILNS.toString()).isEqualTo("FAIL");
    }

    @Test
    public void testScreenshotToString() {
        assertThat(Status.SCREENSHOT.toString()).isEqualTo("SCREENSHOT");
    }

    @Test
    public void testDebugToString() {
        assertThat(Status.DEBUG.toString()).isEqualTo("DEBUG");
    }

    @Test
    public void testWarningToString() {
        assertThat(Status.WARNING.toString()).isEqualTo("WARNING");
    }

    @Test
    public void testCompleteToString() {
        assertThat(Status.COMPLETE.toString()).isEqualTo("COMPLETE");
    }

    // ---- getValue ----

    @Test
    public void testGetValueTrue() {
        assertThat(Status.getValue(true)).isEqualTo(Status.PASS);
    }

    @Test
    public void testGetValueFalse() {
        assertThat(Status.getValue(false)).isEqualTo(Status.FAIL);
    }

    // ---- Enum values count ----

    @Test
    public void testEnumValuesCount() {
        assertThat(Status.values()).hasSize(9);
    }

    // ---- valueOf ----

    @Test
    public void testValueOf() {
        assertThat(Status.valueOf("PASS")).isEqualTo(Status.PASS);
        assertThat(Status.valueOf("FAIL")).isEqualTo(Status.FAIL);
        assertThat(Status.valueOf("DONE")).isEqualTo(Status.DONE);
        assertThat(Status.valueOf("COMPLETE")).isEqualTo(Status.COMPLETE);
    }
}
