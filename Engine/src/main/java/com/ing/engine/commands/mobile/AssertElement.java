package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.exception.ForcedException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import org.openqa.selenium.JavascriptExecutor;

public class AssertElement extends MobileGeneral {

    public AssertElement(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is not present")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementNotPresent() {
        assertNotElement(!elementPresent());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is not selected")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementNotSelected() {
        assertNotElement(!elementSelected());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is not displayed")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementNotDisplayed() {
        assertNotElement(!elementDisplayed());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is not enabled")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementNotEnabled() {
        assertNotElement(!elementEnabled());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is present")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementPresent() {
        assertElement(elementPresent());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] element is selected")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementSelected() {
        assertElement(elementSelected());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] element is displayed")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementDisplayed() {
        assertElement(elementDisplayed());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is enabled on the current page")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementEnabled() {
        assertElement(elementEnabled());
    }

    private void assertElement(Boolean status, String isNot) {
        String value = isNot + Action.replaceFirst("assertElement", "").replaceFirst("Not", "");
        String description = String.format("Element [%s] is %s", ObjectName, value);
        if (status) {
            Report.updateTestLog(Action, description, Status.PASS);
        } else {
            throw new ForcedException(Action, description);
        }
    }

    private void assertElement(Boolean status) {
        assertElement(status, status ? "" : "not ");
    }

    private void assertNotElement(Boolean status) {
        assertElement(status, status ? "not " : "");
    }
}
