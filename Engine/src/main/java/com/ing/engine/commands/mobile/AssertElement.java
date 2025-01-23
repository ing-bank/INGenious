package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import org.openqa.selenium.JavascriptExecutor;

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

    /*
    @Action(object = ObjectType.MOBILE, desc = "Assert if Page source of current page is: [<Data>]", input = InputType.YES)
    public void assertPageSource() {
        if (mDriver.getPageSource().equals(Data)) {
            Report.updateTestLog(Action, "Current Page Source is matched with the expected Page Source", Status.DONE);
        } else {
            throw new ForcedException(Action, "Current Page Source doesn't match with the expected Page Source");
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Assert if the Horizontal Scrollbar is present")
    public void assertHScrollBarPresent() {
        assertHScorllBar(isHScrollBarPresent());
    }

    @Action(object = ObjectType.MOBILE, desc = "Assert if the Horizontal Scrollbar is not present")
    public void assertHScrollBarNotPresent() {
        assertHScorllBar(isHScrollBarPresent());
    }

    @Action(object = ObjectType.MOBILE, desc = "Assert if the Vertical Scrollbar is present")
    public void assertVScrollBarPresent() {
        assertVScorllBar(isvScrollBarPresent());
    }

    @Action(object = ObjectType.MOBILE, desc = "Assert if the Vertical Scrollbar is not present")
    public void assertVScrollBarNotPresent() {
        assertVScorllBar(isvScrollBarPresent());
    }

    private void assertHScorllBar(Boolean value) {
        assertScorllBar("Horizontal", value);
    }

    private void assertVScorllBar(Boolean value) {
        assertScorllBar("Vertical", value);
    }

    private void assertScorllBar(String type, Boolean value) {
        if (value) {
            String desc = type + " Scrollbar is " + " present";
            if (Action.contains("Not")) {
                Report.updateTestLog(Action, desc, Status.FAIL);
            } else {
                Report.updateTestLog(Action, desc, Status.PASS);
            }
        } else {
            String desc = type + " Scrollbar is not" + " present";
            if (Action.contains("Not")) {
                Report.updateTestLog(Action, desc, Status.PASS);
            } else {
                Report.updateTestLog(Action, desc, Status.FAIL);
            }
        }
    }
     */
}
