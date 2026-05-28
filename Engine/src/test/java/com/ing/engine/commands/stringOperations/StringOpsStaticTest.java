package com.ing.engine.commands.stringOperations;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

/**
 * Tests for the static utility methods on StringOperations
 * that don't require CommandControl dependencies.
 */
public class StringOpsStaticTest {

    @Test
    public void testCountCharOccurrencesSingleMatch() {
        assertThat(StringOperations.countCharOccurrences("hello", 'l')).isEqualTo(2);
    }

    @Test
    public void testCountCharOccurrencesNoMatch() {
        assertThat(StringOperations.countCharOccurrences("hello", 'z')).isEqualTo(0);
    }

    @Test
    public void testCountCharOccurrencesAllMatch() {
        assertThat(StringOperations.countCharOccurrences("aaaa", 'a')).isEqualTo(4);
    }

    @Test
    public void testCountCharOccurrencesEmpty() {
        assertThat(StringOperations.countCharOccurrences("", 'a')).isEqualTo(0);
    }

    @Test
    public void testCountCharOccurrencesSpecialChars() {
        assertThat(StringOperations.countCharOccurrences("a.b.c.d", '.')).isEqualTo(3);
    }

    @Test
    public void testCountCharOccurrencesSpaces() {
        assertThat(StringOperations.countCharOccurrences("one two three", ' ')).isEqualTo(2);
    }
}
