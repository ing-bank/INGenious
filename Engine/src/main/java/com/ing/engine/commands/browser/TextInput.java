package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.ing.util.encryption.Encryption;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TextInput extends General {

    public TextInput(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in the Field [<Object>]", input = InputType.YES)
    public void Fill() {
        try {
            Locator.clear();
            Locator.fill(Data);
            Report.updateTestLog(Action, "Entered Text '" + Data + "' on '"
                    + "[" + ObjectName + "]" + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in the Field [<Object>]", input = InputType.YES)
    public void PressSequentially() {
        try {
            Locator.clear();
            Locator.pressSequentially(Data);
            Report.updateTestLog(Action, "Entered Text '" + Data + "' on '"
                    + "[" + ObjectName + "]" + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in the [<Object>] if it Data exists", input = InputType.YES)
    public void FillIfDataExists() {
        Page.waitForLoadState();
        if (!Data.isEmpty()) {
            Fill();
        } else {
            Report.updateTestLog(Action, "Data not present", Status.DONE);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in the [<Object>] if visible", input = InputType.YES)
    public void FillIfVisible() {
        Page.waitForLoadState();
        if (Locator.isVisible()) {
            Fill();
        } else {
            Report.updateTestLog(Action, "[" + ObjectName + "]" + " is not visible", Status.DONE);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in the Field [<Object>] and check [<Data>] matches with [<Object>] value", input = InputType.YES)
    public void FillAndCheck() {
        try {
            Locator.clear();
            Locator.fill(Data);
            if (Locator.getAttribute("value").equals(Data)) {
                Report.updateTestLog("Set", "Entered Text '" + Data + "' on '"
                        + "[" + ObjectName + "]" + "'", Status.DONE);
            } else {
                Report.updateTestLog("Set", "Unable Enter Text '" + Data
                        + "' on '" + ObjectName + "'", Status.FAIL);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Clear text [<Data>] from object [<Object>].")
    public void Clear() {
        try {
            Locator.clear();
            Report.updateTestLog("Clear", "Cleared Text on '" + "[" + ObjectName + "]" + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the Decrypted value [<Data>] in the Field [<Object>]", input = InputType.YES)
    public void fillEncrypted() {
        if (Data != null && Data.matches(".* Enc")) {
            try {
                Locator.clear();
                Data = Data.substring(0, Data.lastIndexOf(" Enc"));
                byte[] valueDecoded = Encryption.getInstance().decrypt(Data).getBytes();
                Locator.fill(new String(valueDecoded));
                Report.updateTestLog(Action, "Entered Encrypted Text " + Data + " on " + "[" + ObjectName + "]", Status.DONE);
            } catch (Exception ex) {
                Report.updateTestLog(Action, ex.getMessage(), Status.FAIL);
                Logger.getLogger(TextInput.class.getName()).log(Level.SEVERE, null, ex);
                throw new ActionException(ex);
            }

        } else {
            Report.updateTestLog(Action, "Data not encrypted '" + Data + "'", Status.DEBUG);
        }
    }

}
