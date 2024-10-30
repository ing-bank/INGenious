
package com.ing.engine.galenWrapper;

import com.galenframework.api.Galen;
import com.galenframework.utils.GalenUtils;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.WebDriver;

public class GalenWrapper extends Galen {

    public synchronized static void dumpPage(PageValidationWrapper pageValidation, String pageName, String testCaseName, File reportFolder) throws IOException {
        GalenPageDumpWrapper dump = new GalenPageDumpWrapper(pageName);
        dump.dumpPage(pageValidation, testCaseName, reportFolder);
    }

    public synchronized static File takeScreenShot(WebDriver Driver) {
        try {
            return GalenUtils.makeFullScreenshot(Driver);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(GalenWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
