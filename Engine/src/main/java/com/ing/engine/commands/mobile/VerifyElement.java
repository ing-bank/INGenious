package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

/*
public class VerifyElement extends MobileGeneral {

    public VerifyElement(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.APP, desc = "Verify if [<Object>] element is not present")
    public void verifyElementNotPresent() {
        verifyNotElement(!elementPresent());
    }

    @Action(object = ObjectType.APP, desc = "Verify if [<Object>] element is not selected")
    public void verifyElementNotSelected() {
        verifyNotElement(!elementSelected());
    }

    @Action(object = ObjectType.APP, desc = "Verify if [<Object>] element is not displayed")
    public void verifyElementNotDisplayed() {
        verifyNotElement(!elementDisplayed());
    }

    @Action(object = ObjectType.APP, desc = "Verify if [<Object>] element is not enabled")
    public void verifyElementNotEnabled() {
        verifyNotElement(!elementEnabled());
    }

    @Action(object = ObjectType.APP, desc = "Verify if [<Object>] element is present")
    public void verifyElementPresent() {
        verifyElement(elementPresent());
    }

    @Action(object = ObjectType.APP, desc = "Verify if [<Object>] element is selected")
    public void verifyElementSelected() {
        verifyElement(elementSelected());
    }

    @Action(object = ObjectType.APP, desc = "Verify if [<Object>] element is displayed")
    public void verifyElementDisplayed() {
        verifyElement(elementDisplayed());
    }

    @Action(object = ObjectType.APP, desc = "Verify if [<Object>] element is enabled")
    public void verifyElementEnabled() {
        verifyElement(elementEnabled());
    }

    private void verifyElement(Boolean status, String isNot) {
        String value = isNot + Action.replaceFirst("verifyElement", "").replaceFirst("Not", "");
        String description = String.format("Element [%s] is %s", ObjectName, value);
        Report.updateTestLog(Action, description, Status.getValue(status));
    }

    private void verifyElement(Boolean status) {
        verifyElement(status, status ? "" : "not ");
    }

    private void verifyNotElement(Boolean status) {
        verifyElement(status, status ? "not " : "");
    }

    @Action(object = ObjectType.MOBILE, desc = "Verify if Page source of current page is: [<Data>]", input = InputType.YES)
    public void verifyPageSource() {
        boolean value = mDriver.getPageSource().equals(Data);
        Report.updateTestLog(
                Action,
                "Current Page Source is" + (value ? "" : " not") + " matched with the expected Page Source",
                Status.getValue(value));
    }

    /*
    @Action(object = ObjectType.MOBILE, desc = "Verify if the HScrollBar is present")
    public void verifyHScrollBarPresent() {
        verifyHScorllBar("", isHScrollBarPresent());
    }

    @Action(object = ObjectType.MOBILE, desc = "Verify if the HScrollBar is not present")
    public void verifyHScrollBarNotPresent() {
        verifyHScorllBar("not", !isHScrollBarPresent());
    }

    @Action(object = ObjectType.MOBILE, desc = "Verify if the VScrollBar is present")
    public void verifyVScrollBarPresent() {
        verifyVScorllBar("", isvScrollBarPresent());
    }

    @Action(object = ObjectType.MOBILE, desc = "Verify if the VScrollBar is not present")
    public void verifyVScrollBarNotPresent() {
        verifyVScorllBar("not", !isvScrollBarPresent());
    }

    private void verifyHScorllBar(String isNot, Boolean value) {
        verifyScorllBar("Horizontal", isNot, value);
    }

    private void verifyVScorllBar(String isNot, Boolean value) {
        verifyScorllBar("Vertical", isNot, value);
    }

    private void verifyScorllBar(String type, String isNot, Boolean value) {
        String desc = type + " Scrollbar is " + isNot + " present";
        Report.updateTestLog(Action, desc, Status.getValue(value));
    }
    
    
   

}
*/