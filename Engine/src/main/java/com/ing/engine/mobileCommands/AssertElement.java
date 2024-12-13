package com.ing.engine.mobileCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

public class AssertElement extends MobileGeneral {

    public AssertElement(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is not present")
    public void assertElementNotPresent() {
        assertNotElement(!elementPresent());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is not selected")
    public void assertElementNotSelected() {
        assertNotElement(!elementSelected());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is not displayed")
    public void assertElementNotDisplayed() {
        assertNotElement(!elementDisplayed());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is not enabled")
    public void assertElementNotEnabled() {
        assertNotElement(!elementEnabled());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is present")
    public void assertElementPresent() {
        assertElement(elementPresent());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] element is selected")
    public void assertElementSelected() {
        assertElement(elementSelected());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] element is displayed")
    public void assertElementDisplayed() {
        assertElement(elementDisplayed());
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is enabled on the current page")

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
