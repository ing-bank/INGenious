
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.execution.exception.ActionException;
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

/**
 *
 * 
 */
public class Performance extends Command {

    public Performance(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.BROWSER, desc = "To delete all the cookies", input = InputType.NO)
    public void clearCache() {
        try {
            Page.context().clearCookies();
            Report.updateTestLog(Action, "Cookies Cleared", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Failed to clear cookies", Status.DONE);
            throw new ActionException(e);
        }
    }

    /**
     * capture browser page navigation and resource timings and store it in the
     * report object
     */
    @Action(object = ObjectType.BROWSER, desc = "Capture the PageTimings for the Page [<Data>]", input = InputType.YES)
    public void capturePageTimings() {
        if (Control.exe.getExecSettings().getRunSettings().isPerformanceLogEnabled()) {
            try {
                storePageTimings();
                Report.updateTestLog(Action, "Timings Updated in Har", Status.DONE);
            } catch (Exception ex) {
                Report.updateTestLog(Action, "Unable to update PageTimings : " + ex.getMessage(),
                        Status.FAIL);
                Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                throw new ActionException(ex);
            }
        }
    }

    private void storePageTimings() {
        
        String pt = (String) Page.evaluate(PerformanceTimings.script());
        String rt = "[]";
        try {
            rt = (String) Page.evaluate(ResourceTimings.script());
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
        return escapeName(Data.isEmpty() ? (Page.title()) : Data);
    }

    private String escapeName(String data) {
        return Objects.toString(data, "")
                .replaceAll("[^a-zA-Z0-9-]", "_").replaceAll("__+", "_");
    }

}
