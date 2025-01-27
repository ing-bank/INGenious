
package com.ing.engine.commands.galenCommands;

import com.ing.engine.constants.FilePath;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.galenWrapper.GalenWrapper;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *
 * 
 */
public class PageDump extends General {

    public PageDump(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.BROWSER, desc = "Creates Page dump for Page [<Data>]", input = InputType.YES)
    public void createPageDump() {
        checkIfDumpResourceExists();
        Reference = Data;
        Condition = "(.*)";
        try {
            GalenWrapper.dumpPage(getPageValidation(RelativeElement.WebElementList),
                    Data,
                    Report.getTestCaseName(),
                    new File(FilePath.getPageDumpLocation() + File.separator + Report.getTestCaseName() + File.separator + Data));
            Report.updateTestLog(Action, "Page Dump created for page" + Data, Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Page Dump creation -Failed " + ex.getMessage(), Status.DEBUG);
            Logger.getLogger(PageDump.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void checkIfDumpResourceExists() {
        File file = new File(Control.getCurrentProject().getLocation() + File.separator + "PageDump");
        if (!file.exists() || file.listFiles() == null || file.listFiles().length == 0) {
            file.mkdirs();
            try {
                FileUtils.copyDirectory(new File(FilePath.getPageDumpResourcePath()), file);
            } catch (IOException ex) {
                Logger.getLogger(PageDump.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
*/
}
