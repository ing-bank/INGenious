
package com.ing.engine.reporting.performance.metrics;

import com.ing.engine.constants.FilePath;
import com.ing.engine.reporting.performance.PerformanceReport;
import com.ing.engine.reporting.performance.har.Har;
import com.ing.engine.reporting.performance.metrics.pagespeed.PageSpeed;
import java.io.File;
import org.json.simple.JSONObject;

/**
 *
 * 
 */
public class MetricsProvider {

    private static final File H_PS_EXE = new File(FilePath.getConfigurationPath(), "har_to_pagespeed.exe");

    public static final String MODULE = "perf.metrics.module";

    public static final void init() {
        switch (getProvider()) {
            default:
                break;
        }
    }

    public enum Module {
        DEF;

        public static Module toModule(String m) {
            try {
                return valueOf(m.trim());
            } catch (Exception ex) {
                return DEF;
            }
        }

        public static String getConfig(Module m) {
            switch (m) {
                default:
                    return FilePath.getConfigurationPath();
            }

        }
    }

    public static PageMetrics getMetrics(Har<?, ?> har, int i, String harName,
            PerformanceReport.Report r) {
        PageMetrics metrics;
        switch (getProvider()) {
            default:
                metrics = new PageSpeed(((JSONObject) har.log().pages.get(0)).
                        get("title").toString() + " " + harName,
                        r.savedHars.get(harName).get(i), H_PS_EXE);
                break;
        }
        return metrics;
    }

    private static Module getProvider() {
        return Module.toModule(
                System.getProperty(MODULE, Module.DEF.name())
                        .toUpperCase()
        );
    }

    public static boolean isModule(Module m) {
        return getProvider().equals(m);
    }

}
