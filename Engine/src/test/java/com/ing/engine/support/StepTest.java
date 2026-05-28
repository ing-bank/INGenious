package com.ing.engine.support;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

/**
 * Tests for Step factory methods and builder pattern.
 * Tests focus on static factory methods and builder-style setters
 * that don't require TestCaseRunner.
 */
public class StepTest {

    @Test
    public void testConstructorWithStepNum() {
        Step s = new Step(5);
        assertThat(s.StepNum).isEqualTo(5);
    }

    @Test
    public void testCreateWithNum() {
        Step s = Step.create(10);
        assertThat(s.StepNum).isEqualTo(10);
        assertThat(s.ObjectName).isEqualTo("Browser");
        assertThat(s.Condition).isEmpty();
        assertThat(s.Input).isEmpty();
        assertThat(s.Data).isEmpty();
    }

    @Test
    public void testCreateNoArgs() {
        Step s = Step.create();
        assertThat(s.StepNum).isEqualTo(-1);
        assertThat(s.ObjectName).isEqualTo("Browser");
    }

    @Test
    public void testObjectSetter() {
        Step s = Step.create(1);
        Step result = s.object("Login", "LoginPage");
        assertThat(result).isSameAs(s);
        assertThat(s.ObjectName).isEqualTo("Login");
        assertThat(s.Reference).isEqualTo("LoginPage");
    }

    @Test
    public void testObjectSetterOneArg() {
        Step s = Step.create(1);
        s.object("Btn");
        assertThat(s.ObjectName).isEqualTo("Btn");
        assertThat(s.Reference).isEmpty();
    }

    @Test
    public void testActionSetter() {
        Step s = Step.create(1);
        Step result = s.action("click");
        assertThat(result).isSameAs(s);
        assertThat(s.Action).isEqualTo("click");
    }

    @Test
    public void testConditionSetter() {
        Step s = Step.create(1);
        Step result = s.condition("isVisible");
        assertThat(result).isSameAs(s);
        assertThat(s.Condition).isEqualTo("isVisible");
    }

    @Test
    public void testInputSetter() {
        Step s = Step.create(1);
        Step result = s.input("Hello World");
        assertThat(result).isSameAs(s);
        assertThat(s.Input).isEqualTo("Hello World");
        assertThat(s.Data).isEqualTo("Hello World");
    }

    @Test
    public void testExecuteClassSetter() {
        Step s = Step.create(1);
        Step result = s.executeClass("com.example:doStuff");
        assertThat(result).isSameAs(s);
        assertThat(s.ObjectName).isEqualTo("ExecuteClass");
        assertThat(s.Action).isEqualTo("com.example:doStuff");
        assertThat(s.Reference).isEmpty();
    }

    @Test
    public void testExecuteStringSetter() {
        Step s = Step.create(1);
        Step result = s.execute("doSomething");
        assertThat(result).isSameAs(s);
        assertThat(s.ObjectName).isEqualTo("Execute");
        assertThat(s.Action).isEqualTo("doSomething");
    }

    @Test
    public void testFluentChaining() {
        Step s = Step.create(1)
                .object("Button", "Page1")
                .action("click")
                .condition("isEnabled")
                .input("test");
        assertThat(s.ObjectName).isEqualTo("Button");
        assertThat(s.Reference).isEqualTo("Page1");
        assertThat(s.Action).isEqualTo("click");
        assertThat(s.Condition).isEqualTo("isEnabled");
        assertThat(s.Input).isEqualTo("test");
    }

    @Test
    public void testBreakPointDefault() {
        Step s = new Step(1);
        assertThat(s.BreakPoint).isFalse();
    }
}
