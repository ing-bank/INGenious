package com.ing.ingenious.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ActionExceptionTest {

    @Test
    void constructorShouldStoreCauseAndDescriptionFromCauseMessage() {
        RuntimeException cause = new RuntimeException("Action failed");

        ActionException exception = new ActionException(cause);

        assertSame(cause, exception.getCause());
        assertEquals("Action failed", exception.ErrorDescription);
    }
}
