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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.By;

//import org.openqa.selenium.JavascriptExecutor;

public class Assertions extends MobileGeneral {

    public Assertions(CommandControl cc) {
        super(cc);
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Assert if cookie name: [<Data>] is present",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertCookiePresent() {
        try {
            String strCookieName = Data;
            if ((mDriver.manage().getCookieNamed(strCookieName) != null)) {
                System.out.println("assertCookiePresent Passed");
                Report.updateTestLog(
                    "assertCookiePresent",
                    "Cookie name matched with the data provided",
                    Status.PASS
                );
            } else {
                throw new Exception("Cookie name did not match with data provided");
            }
        } catch (Exception ex) {
            System.out.println("assertCookiePresent Failed");
            Logger.getLogger(Assertions.class.getName()).log(Level.SEVERE, null, ex);
            throw new ForcedException("assertCookiePresent", ex.getMessage());
        }
    }
}
