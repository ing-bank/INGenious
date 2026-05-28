package com.ing.storywriter.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class ValidatorTest {

    // ---- Valid names ----

    @Test
    public void testSimpleValidName() {
        assertThat(Validator.isValidName("MyTestCase")).isTrue();
    }

    @Test
    public void testValidNameWithSpaces() {
        assertThat(Validator.isValidName("My Test Case")).isTrue();
    }

    @Test
    public void testValidNameWithNumbers() {
        assertThat(Validator.isValidName("Test123")).isTrue();
    }

    @Test
    public void testValidNameWithUnderscore() {
        assertThat(Validator.isValidName("test_case")).isTrue();
    }

    @Test
    public void testValidNameWithHyphen() {
        assertThat(Validator.isValidName("test-case")).isTrue();
    }

    @Test
    public void testValidNameWithDot() {
        assertThat(Validator.isValidName("file.txt")).isTrue();
    }

    // ---- Invalid names (reserved Windows names) ----

    @Test
    public void testReservedNameCON() {
        assertThat(Validator.isValidName("CON")).isFalse();
    }

    @Test
    public void testReservedNamePRN() {
        assertThat(Validator.isValidName("PRN")).isFalse();
    }

    @Test
    public void testReservedNameAUX() {
        assertThat(Validator.isValidName("AUX")).isFalse();
    }

    @Test
    public void testReservedNameNUL() {
        assertThat(Validator.isValidName("NUL")).isFalse();
    }

    @Test
    public void testReservedNameCOM1() {
        assertThat(Validator.isValidName("COM1")).isFalse();
    }

    @Test
    public void testReservedNameLPT1() {
        assertThat(Validator.isValidName("LPT1")).isFalse();
    }

    @Test
    public void testReservedNameCaseInsensitive() {
        assertThat(Validator.isValidName("con")).isFalse();
        assertThat(Validator.isValidName("nul")).isFalse();
    }

    // ---- Invalid names (excluded characters) ----

    @Test
    public void testExcludeComma() {
        assertThat(Validator.isValidName("test,name")).isFalse();
    }

    @Test
    public void testExcludeHash() {
        assertThat(Validator.isValidName("test#name")).isFalse();
    }

    @Test
    public void testExcludeDollar() {
        assertThat(Validator.isValidName("test$name")).isFalse();
    }

    @Test
    public void testExcludeCurlyBraces() {
        assertThat(Validator.isValidName("test{name}")).isFalse();
    }

    @Test
    public void testExcludeCaret() {
        assertThat(Validator.isValidName("test^name")).isFalse();
    }

    @Test
    public void testExcludeSquareBrackets() {
        assertThat(Validator.isValidName("test[0]")).isFalse();
    }

    @Test
    public void testExcludePercent() {
        assertThat(Validator.isValidName("test%name")).isFalse();
    }

    // ---- Invalid names (Windows invalid chars) ----

    @Test
    public void testInvalidAngleBrackets() {
        assertThat(Validator.isValidName("test<name>")).isFalse();
    }

    @Test
    public void testInvalidColon() {
        assertThat(Validator.isValidName("test:name")).isFalse();
    }

    @Test
    public void testInvalidQuote() {
        assertThat(Validator.isValidName("test\"name")).isFalse();
    }

    @Test
    public void testInvalidPipe() {
        assertThat(Validator.isValidName("test|name")).isFalse();
    }

    @Test
    public void testInvalidQuestionMark() {
        assertThat(Validator.isValidName("test?name")).isFalse();
    }

    @Test
    public void testInvalidAsterisk() {
        assertThat(Validator.isValidName("test*name")).isFalse();
    }

    // ---- Edge cases ----

    @Test
    public void testEndingWithDotInvalid() {
        assertThat(Validator.isValidName("test.")).isFalse();
    }

    @Test
    public void testEndingWithSpaceInvalid() {
        assertThat(Validator.isValidName("test ")).isFalse();
    }
}
