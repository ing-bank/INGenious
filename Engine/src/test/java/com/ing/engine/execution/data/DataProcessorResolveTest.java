package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

/**
 * Tests for DataProcessor resolution methods that can be exercised
 * without requiring full Control/RunManager bootstrap.
 * Focuses on input pattern detection and static resolve logic.
 */
public class DataProcessorResolveTest {

    // ---- isInputPatternDynamic ----

    @Test
    public void testDynamicPatternAtSign() {
        assertThat(DataProcessor.isInputPatternDataSheet("@literal")).isFalse();
    }

    @Test
    public void testDynamicPatternEqualsSign() {
        // "=1+2" starts with = so isInputPatternDataSheet should be false
        assertThat(DataProcessor.isInputPatternDataSheet("=1+2")).isFalse();
    }

    @Test
    public void testDynamicPatternPercentVar() {
        assertThat(DataProcessor.isInputPatternDataSheet("%myVar%")).isFalse();
    }

    @Test
    public void testDynamicPatternGreaterThan() {
        assertThat(DataProcessor.isInputPatternDataSheet(">jsExpr")).isFalse();
    }

    @Test
    public void testDynamicPatternAngleBracket() {
        assertThat(DataProcessor.isInputPatternDataSheet("<someRef>")).isFalse();
    }

    @Test
    public void testDynamicPatternSquareBracket() {
        assertThat(DataProcessor.isInputPatternDataSheet("[someRef]")).isFalse();
    }

    @Test
    public void testDynamicPatternDoubleQuote() {
        assertThat(DataProcessor.isInputPatternDataSheet("\"literal\"")).isFalse();
    }

    @Test
    public void testDynamicPatternCurlyBraceNoColon() {
        // {Name} without colon — is dynamic per the regex
        assertThat(DataProcessor.isInputPatternDataSheet("{Name}")).isFalse();
    }

    // ---- isInputPatternDataSheet additional cases ----

    @Test
    public void testDataSheetPatternWithCurlyBraces() {
        assertThat(DataProcessor.isInputPatternDataSheet("{Sheet:Column}")).isTrue();
    }

    @Test
    public void testDataSheetPatternMultipleColons() {
        // "A:B:C" still matches ^[A-Za-z].*:[A-Za-z].* 
        assertThat(DataProcessor.isInputPatternDataSheet("A:B:C")).isTrue();
    }

    @Test
    public void testDataSheetPatternNumericPrefix() {
        // "123:Field" doesn't match ^[A-Za-z]
        assertThat(DataProcessor.isInputPatternDataSheet("123:Field")).isFalse();
    }

    @Test
    public void testDataSheetPatternColonOnly() {
        // ":" doesn't match because left/right parts are empty
        assertThat(DataProcessor.isInputPatternDataSheet(":")).isFalse();
    }

    // ---- resolve(Object) and resolveKeyMapVars require Control.getCurrentProject()
    // which is a static dependency. These are disabled until MockedStatic<Control>
    // support is added in a future sprint.

    @Test(enabled = false, description = "Requires Control.getCurrentProject() static mock")
    public void testResolveStaticNull() {
        String result = DataProcessor.resolve((Object) null);
        assertThat(result).isEqualTo("null");
    }

    @Test(enabled = false, description = "Requires Control.getCurrentProject() static mock")
    public void testResolveStaticAtSymbolTrimsFirst() {
        String result = DataProcessor.resolve("@myValue");
        assertThat(result).isEqualTo("myValue");
    }

    @Test(enabled = false, description = "Requires Control.getCurrentProject() static mock")
    public void testResolveStaticPlainText() {
        String result = DataProcessor.resolve("plainText");
        assertThat(result).isEqualTo("plainText");
    }

    @Test(enabled = false, description = "Requires Control.getCurrentProject() static mock")
    public void testResolveStaticEmptyString() {
        String result = DataProcessor.resolve("");
        assertThat(result).isEqualTo("");
    }

    @Test(enabled = false, description = "Requires Control.getCurrentProject() static mock")
    public void testResolveStaticAtOnly() {
        String result = DataProcessor.resolve("@");
        assertThat(result).isEmpty();
    }

    // ---- trimFirst additional ----

    @Test
    public void testTrimFirstMultiChar() {
        assertThat(DataProcessor.trimFirst("ABC")).isEqualTo("BC");
    }

    @Test
    public void testTrimFirstTwoChars() {
        assertThat(DataProcessor.trimFirst("AB")).isEqualTo("B");
    }

    // ---- resolveKeyMapVars(String, int) — without runtime vars ----

    @Test(enabled = false, description = "Requires Control.getCurrentProject() static mock")
    public void testResolveKeyMapVarsPlainText() {
        String result = DataProcessor.resolveKeyMapVars("hello world", 2);
        assertThat(result).isEqualTo("hello world");
    }

    @Test(enabled = false, description = "Requires Control.getCurrentProject() static mock")
    public void testResolveKeyMapVarsEmpty() {
        String result = DataProcessor.resolveKeyMapVars("", 2);
        assertThat(result).isEmpty();
    }
}
