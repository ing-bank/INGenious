package com.ing.ingenious.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DMLResultTest {

    @Test
    void constructorAndGettersShouldExposeProvidedValues() {
        DMLResult successResult = new DMLResult(true, "UPDATE USERS SET ACTIVE = 1");
        assertTrue(successResult.isSuccess());
        assertEquals("UPDATE USERS SET ACTIVE = 1", successResult.getQuery());

        DMLResult failureResult = new DMLResult(false, "DELETE FROM USERS WHERE ID = 1");
        assertFalse(failureResult.isSuccess());
        assertEquals("DELETE FROM USERS WHERE ID = 1", failureResult.getQuery());
    }
}
