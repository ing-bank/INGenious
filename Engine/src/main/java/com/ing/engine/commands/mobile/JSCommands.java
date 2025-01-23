
package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.ing.util.encryption.Encryption;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.JavascriptExecutor;

/**
 *
 * 
 */
public class JSCommands extends General {

    public JSCommands(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.MOBILE, desc = "Click on [<Object>]")
    public void TapByJS() {
        if (elementPresent()) {
            try {
                JavascriptExecutor js = (JavascriptExecutor) mDriver;
                js.executeScript("arguments[0].click();", Element);
                Report.updateTestLog(Action, "Clicked on " + ObjectName, Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
                Report.updateTestLog(Action, "Couldn't click on " + ObjectName + " - Exception " + ex.getMessage(),
                        Status.FAIL);
            }
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Set encrypted data on [<Object>]", input=InputType.YES)
    public void setEncryptedByJS() {
        if (Data != null && Data.matches(".* Enc")) {
            if (elementEnabled()) {
                try {
                    Data = Data.substring(0, Data.lastIndexOf(" Enc"));
                    byte[] valueDecoded = Encryption.getInstance().decrypt(Data).getBytes();
                    JavascriptExecutor js = (JavascriptExecutor) mDriver;
                    js.executeScript("arguments[0].value='" + new String(valueDecoded) + "'", Element);
                    Report.updateTestLog(Action, "Entered Text '" + Data + "' on '" + ObjectName + "'", Status.DONE);
                } catch (Exception ex) {
                    Report.updateTestLog(Action, ex.getMessage(), Status.FAIL);
                    Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                throw new ElementException(ElementException.ExceptionType.Element_Not_Enabled, ObjectName);
            }
        } else {
            Report.updateTestLog(Action, "Data not encrypted '" + Data + "'", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Set [<Data>] on [<Object>]", input = InputType.YES)
    public void setByJS() {
        if (elementPresent()) {
            try {
                JavascriptExecutor js = (JavascriptExecutor) mDriver;
                js.executeScript("arguments[0].value='" + Data + "'", Element);
                Report.updateTestLog(Action, "Entered Text '" + Data + "' on '" + ObjectName + "'", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
                Report.updateTestLog(Action, "Couldn't set value on " + ObjectName + " - Exception " + ex.getMessage(),
                        Status.FAIL);
            }
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Clear the element [<Object>]")
    public void clearByJS() {
        if (elementPresent()) {
            try {
                JavascriptExecutor js = (JavascriptExecutor) mDriver;
                js.executeScript("arguments[0].value=''", Element);
                Report.updateTestLog(Action, "Cleared value from '" + ObjectName + "'", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
                Report.updateTestLog(Action,
                        "Couldn't clear value on " + ObjectName + " - Exception " + ex.getMessage(), Status.FAIL);
            }
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Select element [<Object>] ", input = InputType.YES)
    public void selectByJS() {
        if (elementPresent()) {
            try {
                JavascriptExecutor js = (JavascriptExecutor) mDriver;
                Object value = js.executeScript(
                        "var options=arguments[0].getElementsByTagName('option');" + "for(var i=0;i<options.length;i++)"
                        + "{" + "var value=options[i].textContent?options[i].textContent:options[i].innerText;"
                        + "if(value.trim()==='" + Data.trim() + "')" + "{"
                        + "options[i].setAttribute('selected','selected');" + "return true;" + "}" + "}"
                        + "return false;",
                        Element);
                if (value != null && value.toString().trim().toLowerCase().equals("true")) {
                    Report.updateTestLog(Action, "Item " + Data + " is selected from" + ObjectName, Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Item " + Data + " is not available in the" + ObjectName, Status.FAIL);
                }
            } catch (Exception ex) {
                Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
                Report.updateTestLog(Action,
                        "Couldn't select value from " + ObjectName + " - Exception " + ex.getMessage(), Status.FAIL);
            }
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "To check if [<Object>] is inside the boundary ")
    public void assertInsideBounds() {
        if (elementPresent()) {
            JavascriptExecutor js = (JavascriptExecutor) mDriver;
            Object value = js.executeScript("" + "return isOutside(arguments[0]);" + "function isOutside(x){"
                    + "     return x.scrollWidth <= x.offsetWidth;" + "}", Element);
            if (value != null && Boolean.valueOf(value.toString())) {
                Report.updateTestLog(Action, "Element " + ObjectName + " is inside bounds", Status.PASS);
            } else {
                Report.updateTestLog(Action, "Element " + ObjectName + " is outside bounds", Status.FAIL);
            }
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
        }

    }

    @Action(object = ObjectType.ANY, desc = "To execute the JavaScript commands", input = InputType.YES)
    public void executeEval() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) mDriver;
            if (Element != null) {
                js.executeScript(Data, Element);
            } else {
                js.executeScript(Data);

            }
            Report.updateTestLog(Action, "Javascript executed", Status.DONE);

        } catch (Exception ex) {
            Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);

            Report.updateTestLog(Action, "Javascript execution failed", Status.DEBUG);

        }
    }
*/
}
