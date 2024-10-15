/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ing.engine.MobileCommands;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ing.engine.commands.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.KeyboardModifier;
import com.microsoft.playwright.options.MouseButton;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MobileMouseClick extends General {

    public MobileMouseClick(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.MOBILE, desc = "Click the [<Object>] ")
    public void Tap() {
        try {
            Element.click();
            Report.updateTestLog(Action, "Clicking on " + "[" + ObjectName + "]", Status.DONE);
        } catch (PlaywrightException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
        }
    }
    

}

