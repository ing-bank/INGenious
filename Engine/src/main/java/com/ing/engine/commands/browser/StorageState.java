package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.ingenious.api.exception.ActionException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Route;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageState extends Command {
    public StorageState(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.BROWSER, desc = "Store Storage State in JSON file", input = InputType.YES)
    public void StoreStorageState() {
        try {
            BrowserContext.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get(Data)));
            Report.updateTestLog(Action, "Storage State successfully stored ", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error storing storage state :" + "\n" + ex.getMessage(), Status.DEBUG);
            throw new ActionException(ex);
        }
    }

}
