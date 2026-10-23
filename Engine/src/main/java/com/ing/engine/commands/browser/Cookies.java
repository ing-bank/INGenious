package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.exception.ActionException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.microsoft.playwright.options.Cookie;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cookies extends General {

    public Cookies(CommandControl cc) {
        super(cc);
    }

    @Action(
        object = ObjectType.BROWSER,
        desc = "Store Cookies in a Variable",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "%cookies%",
        inputHelp = "runtime variable name in %var% format",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required"
    )
    public void storeCookiesInVariable() {
        String strObj = Input;
        String cookieString = "";
        try {
            List<Cookie> cookies = BrowserContext.cookies();
            for (Cookie cookie : cookies) {
                cookieString +=
                    "Name=" +
                    cookie.name +
                    " ; " +
                    "Value=" +
                    cookie.value +
                    " ; " +
                    "Domain=" +
                    cookie.domain +
                    " ; " +
                    "URL=" +
                    cookie.url +
                    " ; " +
                    "Path=" +
                    cookie.path +
                    "\n";
            }
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, cookieString);
                Report.updateTestLog(Action, "Cookies stored in variable", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }

    @Action(
        object = ObjectType.BROWSER,
        desc = "Store raw Cookies JSON in a Variable",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "%rawCookies%",
        inputHelp = "runtime variable name in %var% format",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required"
    )
    public void storeRawCookiesInVariable() {
        String strObj = Input;
        try {
            List<Cookie> cookies = BrowserContext.cookies();
            String rawCookies = new com.google.gson.Gson().toJson(cookies);
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, rawCookies);
                Report.updateTestLog(Action, "Raw cookies JSON stored in variable", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Clear Cookies", input = InputType.NO)
    @Args(
        inputHelp = "no input required",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required"
    )
    public void clearCookies() {
        try {
            BrowserContext.clearCookies();
            Report.updateTestLog(Action, "Cookies clear from the Browser", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }
}
