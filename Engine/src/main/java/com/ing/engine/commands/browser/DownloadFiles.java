
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.Download;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadFiles extends General {


    public DownloadFiles(CommandControl cc) {
        super(cc);
    }

    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Set InputFile path in [<Object>]", input = InputType.YES, condition = InputType.OPTIONAL)
    public void DownloadandSaveAs() {
        String fileName = "";
        try
        {
          Download download  = Page.waitForDownload(() -> Locator.click());
          if (!Condition.isEmpty())
              fileName = Condition;
          else
              fileName = download.suggestedFilename();
          download.saveAs(Paths.get(Data,fileName));
          Report.updateTestLog(Action, "File downloaded at path '" + Data + "'", Status.DONE);
        }
        catch (Exception e){
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }

}
