package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

/**
 * Tests the static utility methods on DataProcessor that do not
 * require Control, RunManager, or TestCaseRunner dependencies.
 */
public class DataProcessorStaticTest {

    // ---- trimFirst ----

    @Test
    public void testTrimFirstRemovesFirstChar() {
        assertThat(DataProcessor.trimFirst("@value")).isEqualTo("value");
    }

    @Test
    public void testTrimFirstSingleChar() {
        assertThat(DataProcessor.trimFirst("@")).isEmpty();
    }

    @Test
    public void testTrimFirstWithLongerString() {
        assertThat(DataProcessor.trimFirst("=1+2")).isEqualTo("1+2");
    }

    // ---- isInputPatternDataSheet ----

    @Test
    public void testIsInputPatternDataSheetSimple() {
        assertThat(DataProcessor.isInputPatternDataSheet("Sheet1:Column1")).isTrue();
    }

    @Test
    public void testIsInputPatternDataSheetBraceNotation() {
        assertThat(DataProcessor.isInputPatternDataSheet("{Sheet1:Column1}")).isTrue();
    }

    @Test
    public void testIsInputPatternDataSheetFalseForDynamic() {
        assertThat(DataProcessor.isInputPatternDataSheet("@value")).isFalse();
    }

    @Test
    public void testIsInputPatternDataSheetFalseForPercentVar() {
        assertThat(DataProcessor.isInputPatternDataSheet("%var%")).isFalse();
    }

    @Test
    public void testIsInputPatternDataSheetFalseForPlainText() {
        assertThat(DataProcessor.isInputPatternDataSheet("plaintext")).isFalse();
    }

    @Test
    public void testIsInputPatternDataSheetFalseForAngleBracket() {
        assertThat(DataProcessor.isInputPatternDataSheet("<variable>")).isFalse();
    }

    @Test
    public void testIsInputPatternDataSheetFalseForEmpty() {
        assertThat(DataProcessor.isInputPatternDataSheet("")).isFalse();
    }
}
