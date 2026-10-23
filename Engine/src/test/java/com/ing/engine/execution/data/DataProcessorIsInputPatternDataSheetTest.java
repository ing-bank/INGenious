package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Regression test for ING-reported bug: "Using Shared Test Data in String Operations within
 * Loops ends the iteration prematurely". Root cause: {@link DataProcessor#isInputPatternDataSheet}
 * incorrectly classified a multi-fragment String Operations Input (a quoted literal plus an
 * embedded {@code {Sheet:Column@Shared}} token, e.g. {@code "+",{TestData0:Data1@Shared}}) as a
 * single bare/braced whole-input data-sheet reference, because its character classes excluded
 * the closing brace {@code }} but not the opening one {@code {} - letting the greedy middle group
 * swallow straight through a leading {@code "+",{} and still find a valid-looking
 * {@code Sheet:Column@Shared}} tail. {@code TestCaseRunner.checkIfLastData} then fed the bogus,
 * comma/quote-polluted "sheet name" parsed from that Input into its next-sub-iteration peek,
 * which reported "sheet not found" and silently ended a dynamic Start Param/End Param loop after
 * its very first sub-iteration - even though the real {@code TestData0} sheet had more rows.
 */
public class DataProcessorIsInputPatternDataSheetTest {

    @Test
    public void wholeInputBareSharedReference_IsRecognized() {
        assertThat(DataProcessor.isInputPatternDataSheet("TestData0:Data1@Shared")).isTrue();
    }

    @Test
    public void wholeInputBracedSharedReference_IsRecognized() {
        assertThat(DataProcessor.isInputPatternDataSheet("{TestData0:Data1@Shared}")).isTrue();
    }

    @Test
    public void wholeInputBareProjectReference_IsRecognized() {
        assertThat(DataProcessor.isInputPatternDataSheet("ProjectSheet:Data1")).isTrue();
    }

    @Test
    public void stringOperationsMultiFragmentInput_WithSharedReference_IsNotAWholeInputReference() {
        // The actual repro Input from the attached sample project's Concat step.
        assertThat(DataProcessor.isInputPatternDataSheet("\"+\",{TestData0:Data1@Shared}"))
            .as(
                "A comma-separated literal-plus-reference Input (String Operations' own grammar) " +
                "must never be mistaken for a single whole-input Sheet:Column reference"
            )
            .isFalse();
    }

    @Test
    public void stringOperationsMultiFragmentInput_WithProjectReference_IsNotAWholeInputReference() {
        assertThat(DataProcessor.isInputPatternDataSheet("\"+\",{ProjectSheet:Data1}")).isFalse();
    }

    @Test
    public void stringOperationsMultiFragmentInput_WithTwoReferences_IsNotAWholeInputReference() {
        assertThat(
                DataProcessor.isInputPatternDataSheet(
                    "{ProjectSheet:Data1},{TestData0:Data1@Shared}"
                )
            )
            .isFalse();
    }
}
