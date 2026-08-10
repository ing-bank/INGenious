package com.ing.ingenious.api.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputTypeTest {

    @Test
    void optionalFlagsShouldBeCorrect() {
        assertTrue(InputType.OPTIONAL.isOptional());
        assertFalse(InputType.OPTIONAL.isMandatory());
        assertFalse(InputType.OPTIONAL.isNotNeeded());
    }

    @Test
    void yesFlagsShouldBeCorrect() {
        assertTrue(InputType.YES.isMandatory());
        assertFalse(InputType.YES.isOptional());
        assertFalse(InputType.YES.isNotNeeded());
    }

    @Test
    void noFlagsShouldBeCorrect() {
        assertTrue(InputType.NO.isNotNeeded());
        assertFalse(InputType.NO.isOptional());
        assertFalse(InputType.NO.isMandatory());
    }
}
