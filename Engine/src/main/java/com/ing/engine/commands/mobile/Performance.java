
package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.performance.PerformanceTimings;
import com.ing.engine.reporting.performance.ResourceTimings;
import com.ing.engine.reporting.performance.har.Entry;
import com.ing.engine.reporting.performance.har.Har;
import com.ing.engine.reporting.performance.har.Har.Log;
import com.ing.engine.reporting.performance.har.Page;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openqa.selenium.JavascriptExecutor;

/**
 *
 * 
 */
public class Performance extends Command {

    public Performance(CommandControl cc) {
        super(cc);
    }

    /*
    @Action(object = ObjectType.MOBILE, desc = "To delete all the cookies", input = InputType.NO)
    public void ClearMobileCache() {
        try {
            mDriver.manage().deleteAllCookies();
            Report.updateTestLog(Action, "Cookies Cleared", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Failed to clear cookies", Status.DONE);
        }
    }


    @Action(object = ObjectType.MOBILE, desc = "Capture the PageTimings for the Page [<Data>]", input = InputType.YES)
    public void captureMobilePageTimings() {
        if (Control.exe.getExecSettings().getRunSettings().isPerformanceLogEnabled()) {
            try {
                storePageTimings();
                Report.updateTestLog(Action, "Timings Updated in Har", Status.DONE);
            } catch (Exception ex) {
                Report.updateTestLog(Action, "Unable to update PageTimings : " + ex.getMessage(),
                        Status.FAIL);
                Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    private void storePageTimings() {
        JavascriptExecutor js = (JavascriptExecutor) mDriver;
        String pt = js.executeScript(PerformanceTimings.script()).toString();
        String rt = "[]";
        try {
            rt = js.executeScript(ResourceTimings.script()).toString();
        } catch (Exception ex) {
            Logger.getLogger(Performance.class.getName()).log(Level.SEVERE,
                    "Error on Resource Timings : {0}", ex.getMessage());
        }
        createHar(pt, rt);
    }

    @SuppressWarnings("rawtypes")
    private void createHar(String pt, String rt) {
        Har<String, Log> har = new Har<>();
        Page p = new Page(pt, har.pages());
        har.addPage(p);
        for (Object res : (JSONArray) JSONValue.parse(rt)) {
            JSONObject jse = (JSONObject) res;
            if (jse.size() > 14) {
                Entry e = new Entry(jse.toJSONString(), p);
                har.addEntry(e);
            }
        }
        har.addRaw(pt, rt);
        Control.ReportManager.addHar(har, (TestCaseReport) Report,
                escapeName(Data));
    }

    private String getPageName() {
        return escapeName(Data.isEmpty() ? mDriver.getTitle() : Data);
    }

    private String escapeName(String data) {
        return Objects.toString(data, "")
                .replaceAll("[^a-zA-Z0-9-]", "_").replaceAll("__+", "_");
    }
*/
}
