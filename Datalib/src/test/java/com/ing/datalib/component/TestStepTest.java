package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class TestStepTest {

    // ---- HEADERS enum tests ----

    @Test
    public void testHeadersEnumValues() {
        TestStep.HEADERS[] headers = TestStep.HEADERS.values();
        assertThat(headers).hasSize(7);
    }

    @Test
    public void testHeadersGetValues() {
        assertThat(TestStep.HEADERS.getValues())
                .containsExactly("Step", "ObjectName", "Description", "Action",
                        "Input", "Condition", "Reference");
    }

    @Test
    public void testHeadersSize() {
        assertThat(TestStep.HEADERS.size()).isEqualTo(7);
    }

    @Test
    public void testHeadersIndex() {
        assertThat(TestStep.HEADERS.Step.getIndex()).isEqualTo(0);
        assertThat(TestStep.HEADERS.ObjectName.getIndex()).isEqualTo(1);
        assertThat(TestStep.HEADERS.Action.getIndex()).isEqualTo(3);
        assertThat(TestStep.HEADERS.Reference.getIndex()).isEqualTo(6);
    }

    // ---- TestStep tests using null TestCase constructor ----

    private TestStep createEmptyStep() {
        return new TestStep(null);
    }

    @Test
    public void testEmptyStepHasCorrectSize() {
        TestStep step = createEmptyStep();
        assertThat(step.size()).isEqualTo(TestStep.HEADERS.size());
    }

    @Test
    public void testSetAndGetObject() {
        TestStep step = createEmptyStep();
        step.setObject("Browser");
        assertThat(step.getObject()).isEqualTo("Browser");
    }

    @Test
    public void testSetAndGetAction() {
        TestStep step = createEmptyStep();
        step.setAction("Click");
        assertThat(step.getAction()).isEqualTo("Click");
    }

    @Test
    public void testSetAndGetInput() {
        TestStep step = createEmptyStep();
        step.setInput("username");
        assertThat(step.getInput()).isEqualTo("username");
    }

    @Test
    public void testSetAndGetCondition() {
        TestStep step = createEmptyStep();
        step.setCondition("Start Loop:3");
        assertThat(step.getCondition()).isEqualTo("Start Loop:3");
    }

    @Test
    public void testSetAndGetReference() {
        TestStep step = createEmptyStep();
        step.setReference("LoginPage");
        assertThat(step.getReference()).isEqualTo("LoginPage");
    }

    @Test
    public void testSetAndGetDescription() {
        TestStep step = createEmptyStep();
        step.setDescription("Click the login button");
        assertThat(step.getDescription()).isEqualTo("Click the login button");
    }

    @Test
    public void testSetAndGetTag() {
        TestStep step = createEmptyStep();
        step.setTag("1");
        assertThat(step.getTag()).isEqualTo("1");
    }

    @Test
    public void testIsReusableStep() {
        TestStep step = createEmptyStep();
        step.setObject("Execute");
        step.setAction("Login:SuccessfulLogin");
        assertThat(step.isReusableStep()).isTrue();
    }

    @Test
    public void testIsReusableStepFalse() {
        TestStep step = createEmptyStep();
        step.setObject("Browser");
        step.setAction("Click");
        assertThat(step.isReusableStep()).isFalse();
    }

    @Test
    public void testIsPageObjectStep() {
        TestStep step = createEmptyStep();
        step.setObject("LoginButton");
        step.setReference("LoginPage");
        assertThat(step.isPageObjectStep()).isTrue();
    }

    @Test
    public void testIsPageObjectStepFalseWhenBrowser() {
        TestStep step = createEmptyStep();
        step.setObject("Browser");
        step.setReference("LoginPage");
        assertThat(step.isPageObjectStep()).isFalse();
    }

    @Test
    public void testIsPageObjectStepFalseWhenNoReference() {
        TestStep step = createEmptyStep();
        step.setObject("LoginButton");
        assertThat(step.isPageObjectStep()).isFalse();
    }

    @Test
    public void testIsTestDataStepWithDataSheetFormat() {
        TestStep step = createEmptyStep();
        step.setInput("Sheet1:Column1");
        assertThat(step.isTestDataStep()).isTrue();
    }

    @Test
    public void testIsTestDataStepFalseWithDynamicPrefix() {
        TestStep step = createEmptyStep();
        step.setInput("@value");
        assertThat(step.isTestDataStep()).isFalse();
    }

    @Test
    public void testIsTestDataStepFalseWithAngleBracket() {
        TestStep step = createEmptyStep();
        step.setInput("<variable>");
        assertThat(step.isTestDataStep()).isFalse();
    }

    @Test
    public void testToggleComment() {
        TestStep step = createEmptyStep();
        step.setTag("1");
        step.toggleComment();
        assertThat(step.getTag()).isEqualTo("//1");
        assertThat(step.isCommented()).isTrue();
        step.toggleComment();
        assertThat(step.getTag()).isEqualTo("1");
        assertThat(step.isCommented()).isFalse();
    }

    @Test
    public void testToggleBreakPoint() {
        TestStep step = createEmptyStep();
        step.setTag("1");
        step.toggleBreakPoint();
        assertThat(step.getTag()).isEqualTo("*1");
        assertThat(step.hasBreakPoint()).isTrue();
        step.toggleBreakPoint();
        assertThat(step.getTag()).isEqualTo("1");
        assertThat(step.hasBreakPoint()).isFalse();
    }

    @Test
    public void testGetReusableData() {
        TestStep step = createEmptyStep();
        step.setObject("Execute");
        step.setAction("Login:SuccessfulLogin");
        String[] data = step.getReusableData();
        assertThat(data).containsExactly("Login", "SuccessfulLogin");
    }

    @Test
    public void testGetReusableDataReturnsNullWhenNotReusable() {
        TestStep step = createEmptyStep();
        step.setObject("Browser");
        step.setAction("Click");
        assertThat(step.getReusableData()).isNull();
    }

    @Test
    public void testGetTestDataFromInput() {
        TestStep step = createEmptyStep();
        step.setInput("Sheet1:Column1");
        String[] data = step.getTestDataFromInput();
        assertThat(data).containsExactly("Sheet1", "Column1");
    }

    @Test
    public void testIsEmpty() {
        TestStep step = createEmptyStep();
        assertThat(step.isEmpty()).isTrue();
    }

    @Test
    public void testIsEmptyFalseWhenHasAction() {
        TestStep step = createEmptyStep();
        step.setAction("Click");
        assertThat(step.isEmpty()).isFalse();
    }

    @Test
    public void testCopyValuesTo() {
        TestStep source = createEmptyStep();
        source.setObject("Browser");
        source.setAction("Click");
        source.setInput("submit");

        TestStep target = createEmptyStep();
        source.copyValuesTo(target);
        assertThat(target.getObject()).isEqualTo("Browser");
        assertThat(target.getAction()).isEqualTo("Click");
        assertThat(target.getInput()).isEqualTo("submit");
    }

    @Test
    public void testIsDuplicate() {
        TestStep a = createEmptyStep();
        a.setObject("Browser");
        a.setAction("Click");

        TestStep b = createEmptyStep();
        b.setObject("Browser");
        b.setAction("Click");

        assertThat(a.isDuplicate(b)).isTrue();
    }

    @Test
    public void testIsDuplicateFalse() {
        TestStep a = createEmptyStep();
        a.setAction("Click");

        TestStep b = createEmptyStep();
        b.setAction("Type");

        assertThat(a.isDuplicate(b)).isFalse();
    }

    @Test
    public void testAsReusableStep() {
        TestStep step = createEmptyStep();
        step.asReusableStep("Login", "SuccessLogin");
        assertThat(step.getObject()).isEqualTo("Execute");
        assertThat(step.getAction()).isEqualTo("Login:SuccessLogin");
    }

    @Test
    public void testAsObjectStep() {
        TestStep step = createEmptyStep();
        step.asObjectStep("SubmitBtn", "LoginPage");
        assertThat(step.getObject()).isEqualTo("SubmitBtn");
        assertThat(step.getReference()).isEqualTo("LoginPage");
    }

    @Test
    public void testToString() {
        TestStep step = createEmptyStep();
        step.setAction("Click");
        step.setObject("Button");
        step.setInput("data");
        assertThat(step.toString()).contains("Click").contains("Button").contains("data");
    }

    @Test
    public void testIsDatabaseStep() {
        TestStep step = createEmptyStep();
        step.setObject("Database");
        assertThat(step.isDatabaseStep()).isTrue();
    }

    @Test
    public void testIsWebserviceStep() {
        TestStep step = createEmptyStep();
        step.setObject("Webservice");
        assertThat(step.isWebserviceStep()).isTrue();
    }

    @Test
    public void testIsBrowserStep() {
        TestStep step = createEmptyStep();
        step.setObject("Browser");
        assertThat(step.isBrowserStep()).isTrue();
    }

    @Test
    public void testIsStringOperationsStep() {
        TestStep step = createEmptyStep();
        step.setObject("String Operations");
        assertThat(step.isStringOperationsStep()).isTrue();
    }
}
