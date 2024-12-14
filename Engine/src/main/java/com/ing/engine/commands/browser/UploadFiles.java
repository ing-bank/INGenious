
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.FileChooser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UploadFiles extends General {


        public UploadFiles(CommandControl cc) {
        super(cc);
    }


    @Action(object = ObjectType.PLAYWRIGHT, desc = "Set Single InputFile path in [<Object>]", input = InputType.YES)
    public void SetInputFile() {
        try
        {
          Locator.setInputFiles(Paths.get(Data));
          Report.updateTestLog(Action, "File Path '" + Data
                        + "' is set on [" + ObjectName +"]", Status.DONE);
        }
        catch (Exception e){
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Set InputFile path in [<Object>]", input = InputType.YES)
    public void FileChooser() {
        try
        {
          FileChooser fileChooser = Page.waitForFileChooser(() -> Locator.click());
          fileChooser.setFiles(Paths.get(Data));
          Report.updateTestLog(Action, "File Path '" + Data
                        + "' is set on [" + ObjectName +"]", Status.DONE);
        }
        catch (Exception e){
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }

    
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Set Multiple InputFile paths in [<Object>]", input = InputType.YES)
    public void SetInputFiles() {
         try
        {
          String paths[] = Data.split("|");
          Path filepaths[] = new Path[paths.length];
          for (int i=0;i<paths.length;i++){
              filepaths[i] = Paths.get(paths[i]);
          }
          Locator.setInputFiles(filepaths);
          Report.updateTestLog(Action, "File Paths '" + Data
                        + "' are set on" + " from list [" + ObjectName +"]", Status.DONE);
        }
         catch (Exception e){
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Remove all selected files from [<Object>]", input = InputType.YES)
    public void RemoveInputFile() {
        try
        {
          Locator.setInputFiles(new Path[0]);
          Report.updateTestLog(Action, "File Path '" + Data
                        + "' is removed from [" + ObjectName +"]", Status.DONE);
        }
        catch (Exception e){
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
    
}
