
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.options.Cookie;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Cookies extends General {

    public Cookies(CommandControl cc) {
        super(cc);
    }

    
    @Action(object = ObjectType.BROWSER, desc = "Store Cookies in a Variable", input = InputType.YES)
    public void storeCookiesInVariable() {
        String strObj = Input;
        String cookieString = "";
        try{
            List<Cookie> cookies = BrowserContext.cookies();
            for (Cookie cookie : cookies)
            {
            	cookieString+="Name="+cookie.name+" ; "+"Value="+cookie.value+" ; "+"Domain="+cookie.domain+" ; "+"URL="+cookie.url+" ; "+"Path="+cookie.path+"\n";
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
    
    @Action(object = ObjectType.BROWSER, desc = "Clear Cookies", input = InputType.NO)
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