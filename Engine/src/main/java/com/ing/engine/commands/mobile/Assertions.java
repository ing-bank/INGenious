package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.By;
//import org.openqa.selenium.JavascriptExecutor;

public class Assertions extends MobileGeneral {

    public Assertions(CommandControl cc) {
        super(cc);
    }

    /**
     * ******************************************
     * Function to assert if a given Text is Present in the WebPage
     * ******************************************
     */
    @Action(object = ObjectType.MOBILE,
            desc = "Assert if text: [<Data>] is present on the page",
            input = InputType.YES)
    public void assertTextPresentInPage() throws RuntimeException {

        try {
            String strObj = Data;
            if (mDriver.findElement(By.tagName("html")).getText()
                    .contains(strObj)) {
                System.out.println("assertTextPresent passed");
                Report.updateTestLog("assertTextPresentInPage",
                        "Expected text '" + strObj
                        + "' is  present in the page", Status.PASS);

            } else {
                System.out.println("assertTextPresentInPage failed");
                throw new Exception("Expected text  '" + strObj
                        + "' is not present in the page");
            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            throw new ForcedException("assertTextPresentInPage", e.getMessage());
        }
    }

    /**
     * ******************************************
     * Function to assert the variable
     * ******************************************
     */
    @Action(object = ObjectType.MOBILE,
            desc = "Assert if Key:Value -> [<Data>] is valid",
            input = InputType.YES)
    public void assertVariable() throws RuntimeException {
        try {
            String strObj = Data;
            String[] strTemp = strObj.split("=", 2);
            String strAns = strTemp[0].matches("%.+%") ? getVar(strTemp[0]) : strTemp[0];
            if (strAns.equals(strTemp[1])) {
                System.out.println("Condition '" + Input + "' is true ");
                Report.updateTestLog("assertVariable",
                        "Variable matched with Provided data", Status.PASS);

            } else {
                System.out.println("Condition '" + Input + "' is false ");
                throw new Exception("Variable did not match with provided data");
            }
        } catch (Exception ex) {
            Logger.getLogger(Assertions.class.getName()).log(Level.SEVERE, null, ex);
            throw new ForcedException("assertVariable", ex.getMessage());
        }
    }

    /**
     * ******************************************
     * Function to assert cookies present
     *
     * ******************************************
     */
    @Action(object = ObjectType.MOBILE, desc = "Assert if cookie name: [<Data>] is present", input = InputType.YES)
    public void assertCookiePresent() {
        try {
            String strCookieName = Data;
            if ((mDriver.manage().getCookieNamed(strCookieName) != null)) {
                System.out.println("assertCookiePresent Passed");
                Report.updateTestLog("assertCookiePresent",
                        "Cookie name matched with the data provided",
                        Status.PASS);
            } else {
                throw new Exception(
                        "Cookie name did not match with data provided");
            }
        } catch (Exception ex) {
            System.out.println("assertCookiePresent Failed");
            Logger.getLogger(Assertions.class.getName()).log(Level.SEVERE, null, ex);
            throw new ForcedException("assertCookiePresent", ex.getMessage());
        }
    }

    /**
     * ******************************************
     * Function to assert cookies by name
     * ******************************************
     */
    @Action(object = ObjectType.MOBILE, desc = "Assert if cookie: [<Object>] has name: [<Data>]", input = InputType.YES)
    public void assertCookieByName() {
        try {

            String strCookieName = Data.split(":", 2)[0];
            String strCookieValue = Data.split(":", 2)[1];
            if (mDriver.manage().getCookieNamed(strCookieName) != null) {
                if ((mDriver.manage().getCookieNamed(strCookieName).getValue()
                        .equals(strCookieValue))) {
                    System.out.println("assertCookieByName Passed");
                    Report.updateTestLog("assertCookieByName",
                            "Cookie name matched with provided data",
                            Status.PASS);

                } else {
                    throw new Exception(
                            "Cookie value did not match with provided data");
                }
            } else {
                throw new Exception("Cookie  with the name '" + strCookieName
                        + "' did not exist");
            }
        } catch (Exception ex) {
            System.out.println("assertCookieByName Failed");
            Logger.getLogger(Assertions.class.getName()).log(Level.SEVERE, null, ex);
            throw new ForcedException("assertCookieByName", ex.getMessage());
        }
    }

    @Action(object = ObjectType.MOBILE,
            desc = "Assert if  the  variable value matches with given value from datasheet(variable:datasheet->  [<Data>] )",
            input = InputType.YES,
            condition = InputType.YES)
    public void assertVariableFromDataSheet() throws RuntimeException {
        try {
            String strAns = getVar(Condition);
            if (strAns.equals(Data)) {
                System.out.println("Variable " + Condition + " equals "
                        + Input);
                Report.updateTestLog(Action,
                        "Variable is matched with the expected result", Status.DONE);

            } else {
                System.out.println("Variable " + Condition + " is not equal "
                        + Input);
                throw new ForcedException(Action,
                        "Variable did not match with provided data");
            }
        } catch (Exception e) {
            Logger.getLogger(Assertions.class.getName()).log(Level.SEVERE, null, e);
            throw new ForcedException("assertVariableFromDataSheet", e.getMessage());
        }
    }

}
