
package com.ing.engine.reporting.performance;

import com.ing.engine.constants.FilePath;
import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.performance.har.Har;
import com.ing.engine.reporting.performance.har.Har.Log;
import com.ing.engine.reporting.performance.metrics.MetricsProvider;
import com.ing.engine.reporting.performance.metrics.PageMetrics;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Performance reporting Har waterfall for timings and pagespeed metrics
 *
 * 
 * 
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PerformanceReport {

    private static final Logger LOG = Logger.getLogger(PerformanceReport.class.getName());
    JSONObject pLog = new JSONObject();
    public Map<String, Report> gdata;

    public class Report {

        public Map<String, List<Har<String, Log>>> hars = new LinkedHashMap<>();
        public Map<String, List<File>> savedHars = new LinkedHashMap<>();
    }

    public PerformanceReport() {
        gdata = new LinkedHashMap<>();
        MetricsProvider.init();
    }

    /**
     * Create new har file .,to be called whenever new page request made
     *
     * @param h har data to add
     * @param report current testcase report
     * @param pageName
     */
    public void addHar(Har<String, Log> h, TestCaseReport report, String pageName) {
        String harName = getFullName(report);
        String group = getName(report.Scenario, report.TestCase);
        Report greport;
        if (gdata.containsKey(group)) {
            greport = gdata.get(group);
        } else {
            greport = new Report();
        }
        gdata.put(group, greport);
        if (greport.hars.get(harName) == null) {
            greport.hars.put(harName, new ArrayList());
            greport.savedHars.put(harName, new ArrayList());
        }
        greport.hars.get(harName).add(h);
        h.updateConfig(report, pageName, greport.hars.get(harName).size());
        if (pageName == null || pageName.isEmpty()) {
            pageName = report.Scenario + "-" + report.TestCase + "_page_" + greport.hars.get(harName).size();
        }
        File dest = new File(FilePath.getCurrentPerfReportHarPath(), pageName);
        if (!dest.exists()) {
            dest.mkdirs();
        }
        File harFile = new File(dest, FilePath.getCurrentReportFolderName() + "_" + harName + "_"
                + greport.hars.get(harName).size() + "_" + pageName + ".har");
        storeHar(harFile, h);
        greport.savedHars.get(harName).add(harFile);
    }

    /**
     * update the har and page speed data after every testcase
     *
     * @param sc
     * @param tc
     */
    public void updateTestCase(String sc, String tc) {
        String group = getName(sc, tc);
        if (!gdata.isEmpty() && gdata.get(group) != null) {
            JSONObject id = new JSONObject();
            pLog.put(group, id);
            System.out.println(new Date() + ": performance log for " + group);
            id.put("har", getHar(group));
            id.put("pSpeed", getpSpeed(group));
            System.out.println();
            System.out.println(new Date() + ": updated performance log for " + group);
        }
    }

    /**
     * export the current data to JSONP file
     */
    public void exportReport() {
        if (!gdata.isEmpty() || !RunManager.getGlobalSettings().isTestRun()) {
            System.out.println(new Date() + ": Exporting performance log ");
            File f = new File(FilePath.getCurrentResultsPath(), "perfLog.js");
            try (PrintWriter w = new PrintWriter(f);) {
                w.write("onPerfLog(" + pLog.toJSONString() + ");");
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
            pLog.clear();
            gdata.clear();
        } else {
            System.out.println("Performance log not found. Make sure \"capturePageTimings\" action is included in testcase. ");
        }
    }

    public JSONArray getHar(String g) {
        JSONArray data = new JSONArray();
        if (gdata.get(g) != null) {
            for (String har : gdata.get(g).hars.keySet()) {
                for (Har h : gdata.get(g).hars.get(har)) {
                    JSONObject harE = new JSONObject();
                    harE.put("name", har);
                    harE.put("har", h);
                    data.add(harE);
                }
            }
        }
        return data;
    }

    /**
     * for all the har files for the current group create page speed metrics and
     * store it in a object
     *
     * @param g group ( scenario + test case)
     * @return page speed data;
     */
    public JSONArray getpSpeed(String g) {
        JSONArray data = new JSONArray();
        ExecutorService executor = Executors.newFixedThreadPool(16);
        List<Future<Object>> results;
        List<PageMetrics> psList = new ArrayList<>();
        if (gdata.get(g) != null) {
            for (String har : gdata.get(g).savedHars.keySet()) {
                List<Har<String, Log>> hars = gdata.get(g).hars.get(har);
                for (int i = 0; i < hars.size(); i++) {
                    PageMetrics metrics = MetricsProvider.getMetrics(hars.get(i), i, har, gdata.get(g));
                    psList.add(metrics);
                }
            }
        }
        try {
            results = executor.invokeAll(psList);
            if (results != null) {
                for (Future fu : results) {
                    if (fu.get() != null) {
                        data.add(fu.get());
                    }
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        executor.shutdown();
        return data;
    }

    

    public void storeHar(File f, Har<?, ?> har) {
        try (PrintWriter w = new PrintWriter(f);) {
            w.write(har.toJSONString());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static String getFullName(TestCaseReport r) {
        return r.Scenario + "-" + r.TestCase + "_[i" + r.getIter() + "  "
                + r.getPlaywrightDriver().getCurrentBrowser() + "v" + r.getPlaywrightDriver().getBrowserVersion()
                + " " + System.getProperty("os.name")+ " " +System.getProperty("os.version")+ " " +System.getProperty("os.arch") + "]";
    }

    private static String getName(String sc, String tc) {
        return sc + "_" + tc;
    }
}
