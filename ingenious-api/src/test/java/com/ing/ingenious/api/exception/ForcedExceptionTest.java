package com.ing.ingenious.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ForcedExceptionTest {

    @Test
    void constructorWithNameAndDescriptionShouldSetFieldsAndMessage() {
        ForcedException exception = new ForcedException("Failure", "Custom description");

        assertEquals("Failure", exception.ErrorName);
        assertEquals("Custom description", exception.ErrorDescription);
        assertEquals("Custom description", exception.getMessage());
    }

    @Test
    void constructorWithDescriptionOnlyShouldSetDefaultName() {
        ForcedException exception = new ForcedException("Only description");

        assertEquals("ForcedException", exception.ErrorName);
        assertEquals("Only description", exception.ErrorDescription);
        assertEquals("Only description", exception.getMessage());
    }

    @Test
    void constructorWithCauseShouldSetCauseAndDescription() {
        IllegalStateException cause = new IllegalStateException("Root cause");

        ForcedException exception = new ForcedException("Failure", cause);

        assertEquals("Failure", exception.ErrorName);
        assertEquals("Root cause", exception.ErrorDescription);
        assertEquals("Root cause", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
