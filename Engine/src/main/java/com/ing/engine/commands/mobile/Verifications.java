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

/*
public class Verifications extends Command {

    public Verifications(CommandControl cc) {
        super(cc);
    }


    @Action(object = ObjectType.MOBILE, desc = "Verify if the title is [<Input>]", input = InputType.YES)
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

    @Action(object = ObjectType.MOBILE, desc = "Verify if the specific alert[<Object>] is present ")
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
*/