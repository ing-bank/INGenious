package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;

public class Verifications extends Command {

    public Verifications(CommandControl cc) {
        super(cc);
    }

    /**
     * ******************************************
     * Function to verify the title
     *
     * ******************************************
     */
    @Action(object = ObjectType.BROWSER, desc = "Verify if the title is [<Input>]", input = InputType.YES)
    public void verifyTitle() {
        String strObj = Data;
        if (mDriver.getTitle().equals(strObj)) {
            System.out.println(Action + " Passed");
            Report.updateTestLog(Action,
                    "Element Title value " + mDriver.getTitle()
                    + " is matched with the expected result",
                    Status.PASS);
        } else {
            System.out.println(Action + " failed");
            Report.updateTestLog(Action,
                    "Element Title value " + mDriver.getTitle()
                    + " doesn't match with the expected result",
                    Status.FAIL);

        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Verify if the specific alert[<Object>] is present ")
    public void verifyAlertPresent() {
        try {
            if ((isAlertPresent(mDriver))) {
                System.out.println(Action + " Passed");
                Report.updateTestLog(Action,
                        "Alert is present",
                        Status.PASSNS);
            } else {
                Report.updateTestLog(Action,
                        "Alert is not present",
                        Status.FAILNS);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(),
                    Status.FAILNS);
            Logger.getLogger(Verifications.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * ******************************************
     * Function to verify variable
     *
     * ******************************************
     */
    @Action(object = ObjectType.BROWSER, desc = "Verify if the specific [<Data>] is present", input = InputType.YES)
    public void verifyVariable() {
        String strObj = Data;
        String[] strTemp = strObj.split("=", 2);
        String strAns = getVar(strTemp[0]);
        if (strAns.equals(strTemp[1])) {
            System.out.println("Variable " + strTemp[0] + " equals "
                    + strTemp[1]);
            Report.updateTestLog(Action,
                    "Variable is matched with the expected result", Status.PASS);
        } else {
            System.out.println("Variable " + strTemp[0] + " not equals "
                    + strTemp[1]);
            Report.updateTestLog(Action,
                    "Variable doesn't match with the expected result",
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Verify of variable [<Data>] from given datasheet", input = InputType.YES, condition = InputType.YES)
    public void verifyVariableFromDataSheet() {
        String strAns = getVar(Condition);
        if (strAns.equals(Data)) {
            System.out.println("Variable " + Condition + " equals "
                    + Input);
            Report.updateTestLog(Action,
                    "Variable is matched with the expected result", Status.DONE);

        } else {
            System.out.println("Variable " + Condition + " is not equal "
                    + Input);
            Report.updateTestLog(Action,
                    "Variable doesn't matched with the expected result",
                    Status.DEBUG);
        }
    }

    private boolean isAlertPresent(WebDriver mDriver) {
        try {
            mDriver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            return false;
        }
    }

}
