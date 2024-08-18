
package com.ing.engine.reporting.performance.har;

import com.ing.engine.reporting.performance.PerformanceTimings;
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Page helper object for har
 *
 * 
 * 
 * @see Har.java
 */
@SuppressWarnings("unchecked")
public class Page extends JSONObject {

    private static final long serialVersionUID = 1L;
    private static final String DF = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    PerformanceTimings pt;

    public Page(String navTimings, int index) {
        super();
        Gson gson = new Gson();
        //parse string(json) result form timings api to #PerformanceTimings.class
        pt = gson.fromJson(navTimings, PerformanceTimings.class);
        put("startedDateTime", getMillstoDate(pt.navigationStart));
        put("id", "page_" + index);
        put("title", pt.url == null ? "" : pt.url);
        put("pageTimings", new PageTimings(pt));
        put("raw",JSONValue.parse(navTimings));        
    }

    public static String getMillstoDate(long nStart) {
        SimpleDateFormat df = new SimpleDateFormat(DF);
        return df.format(new Date(nStart));
    }

    public String getID() {
        return get("id").toString();
    }

    class PageTimings extends JSONObject {

        private static final long serialVersionUID = 1L;

        public PageTimings(PerformanceTimings pt) {
            put("onContentLoad", pt.domContentLoadedEventStart - pt.navigationStart);
            //get max of l.e.start || dom.Complete || dom.c.l.e.end
            put("onLoad", Math.max(pt.loadEventStart,Math.max(pt.domComplete, pt.domContentLoadedEventEnd))
                    - pt.navigationStart);
        }
    }
}
