package com.ing.ingenious.api.status;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusTest {

    @Test
    void toStringShouldNormalizePassAndFailVariants() {
        assertEquals("PASS", Status.PASS.toString());
        assertEquals("PASS", Status.PASSNS.toString());
        assertEquals("FAIL", Status.FAIL.toString());
        assertEquals("FAIL", Status.FAILNS.toString());
    }

    @Test
    void toStringShouldReturnExpectedValuesForOtherStatuses() {
        assertEquals("DONE", Status.DONE.toString());
        assertEquals("SCREENSHOT", Status.SCREENSHOT.toString());
        assertEquals("DEBUG", Status.DEBUG.toString());
        assertEquals("WARNING", Status.WARNING.toString());
        assertEquals("COMPLETE", Status.COMPLETE.toString());
    }

    @Test
    void getValueShouldMapBooleanToStatus() {
        assertEquals(Status.PASS, Status.getValue(true));
        assertEquals(Status.FAIL, Status.getValue(false));
    }
}
