package com.ing.engine.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.Test;

/** Pure parsing/compare tests for {@link NumberConditionEvaluator} — no SAP GUI needed. */
public class NumberConditionEvaluatorTest {

    @Test
    public void parseSplitsOperatorAndExpectedValue() {
        NumberConditionEvaluator.Condition condition = NumberConditionEvaluator.parse("<>0");
        assertThat(condition.operator()).isEqualTo("<>");
        assertThat(condition.expected()).isEqualTo("0");
    }

    @Test
    public void parsePrefersLongerOperatorsFirst() {
        assertThat(NumberConditionEvaluator.parse(">=100").operator()).isEqualTo(">=");
        assertThat(NumberConditionEvaluator.parse("<=100").operator()).isEqualTo("<=");
        assertThat(NumberConditionEvaluator.parse(">100").operator()).isEqualTo(">");
        assertThat(NumberConditionEvaluator.parse("<100").operator()).isEqualTo("<");
    }

    @Test
    public void parseTrimsWhitespaceAroundExpectedValue() {
        NumberConditionEvaluator.Condition condition = NumberConditionEvaluator.parse("==  42  ");
        assertThat(condition.expected()).isEqualTo("42");
    }

    @Test
    public void parseThrowsWhenNoOperatorRecognised() {
        assertThatThrownBy(() -> NumberConditionEvaluator.parse("42"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void evaluateNotEquals() {
        assertThat(NumberConditionEvaluator.evaluate("5", "<>", "0")).isTrue();
        assertThat(NumberConditionEvaluator.evaluate("0", "<>", "0")).isFalse();
    }

    @Test
    public void evaluateEqualsAcceptsBothEqualsSpellings() {
        assertThat(NumberConditionEvaluator.evaluate("10", "=", "10")).isTrue();
        assertThat(NumberConditionEvaluator.evaluate("10", "==", "10")).isTrue();
    }

    @Test
    public void evaluateGreaterAndLess() {
        assertThat(NumberConditionEvaluator.evaluate("150", ">", "100")).isTrue();
        assertThat(NumberConditionEvaluator.evaluate("50", "<", "100")).isTrue();
        assertThat(NumberConditionEvaluator.evaluate("100", ">=", "100")).isTrue();
        assertThat(NumberConditionEvaluator.evaluate("100", "<=", "100")).isTrue();
    }

    @Test
    public void evaluateThrowsNumberFormatExceptionForNonNumericActual() {
        assertThatThrownBy(() -> NumberConditionEvaluator.evaluate("Connection Error", ">", "0"))
            .isInstanceOf(NumberFormatException.class);
    }

    @Test
    public void evaluateExtractsNumberEmbeddedInText() {
        assertThat(NumberConditionEvaluator.evaluate("0 Runtime Errors", "<>", "0")).isFalse();
        assertThat(NumberConditionEvaluator.evaluate("3 Runtime Errors found", "<>", "0")).isTrue();
        assertThat(NumberConditionEvaluator.evaluate("Duration: 1500 ms", ">=", "1000")).isTrue();
    }

    @Test
    public void evaluateExtractsNegativeAndDecimalTokens() {
        assertThat(NumberConditionEvaluator.evaluate("Balance: -12.5 EUR", "<", "0")).isTrue();
    }

    @Test
    public void evaluateThrowsForUnsupportedOperator() {
        assertThatThrownBy(() -> NumberConditionEvaluator.evaluate("1", "~=", "1"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
