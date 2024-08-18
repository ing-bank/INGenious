
package com.ing.engine.cli.lib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * 
 */
@SuppressWarnings("unchecked")
public class Performance {

    /**
     * parse and verify the performance of web pages
     *
     * @param data
     * @param tolerance
     * @return the status pass=>1 , fail=>0
     * @throws Exception
     */
    public static JSONObject checkPageLoading(String data, Integer tolerance) throws Exception {
        String pattern = "onPerfLog\\((.*)\\);";
        Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher m = r.matcher(data);
        if (m.find()) {
            data = m.group(1);
        } else {
            throw new Exception("INVALID_DATA");
        }
        JSONObject ref = getRefPageTimings();
        JSONObject pageMap = getPageMap(data);
        return check(pageMap, ref, tolerance);
    }

    /**
     * iterate and verify page timings with the reference
     *
     * @param pageMap
     * @param refMap
     * @param tolerance
     * @return the status JSONObject;
     */
    private static JSONObject check(JSONObject pageMap, JSONObject refMap, Integer tolerance) {
        JSONObject res = new JSONObject();
        boolean stat = true;
        for (Object page : pageMap.keySet()) {
            JSONArray pageEntries = (JSONArray) pageMap.get(page);
            JSONObject refEntry = (JSONObject) refMap.get(page);
            if (refEntry == null) {
                pageMap.put(page,"NO_REF");
                continue;
            }
            if (tolerance == null) {
                Object t = refEntry.get("tolerance");
                if (t == null) {
                    tolerance = 5;
                } else {
                    tolerance = Integer.valueOf(t + "");
                }
            }
            for (Object e : pageEntries) {
                try {
                    JSONObject entry = (JSONObject) e, pLoad = new JSONObject(), cload = new JSONObject();

                    long val, ref;
                    boolean entryStat = true;
                    val = (long) entry.get("onLoad");
                    ref = (long) refEntry.get("onLoad");
                    if (!accept(val, ref, tolerance, pLoad)) {
                        entryStat = stat = false;
                    }
                    val = (long) entry.get("onContentLoad");
                    ref = (long) refEntry.get("onContentLoad");
                    if (!accept(val, ref, tolerance, cload)) {
                        entryStat = stat = false;
                    }
                    entry.put("status", entryStat);
                    entry.put("onLoad", pLoad);
                    entry.put("onContentLoad", cload);
                } catch (Exception ex) {
                }
            }
        }

        res.put("report", pageMap);
        res.put("status", stat);
        return res;
    }

    private static boolean accept(long val, long ref, int tolerance, JSONObject res) {
        long diff = val - ref;
        long diffPercent = diff / (ref / 100);
        res.put("actual", val);
        res.put("expected", ref);
        res.put("tolerance_%", tolerance);
        res.put("diff_ms", diff);
        res.put("diff_%", diffPercent);
        res.put("status", (tolerance >= diffPercent));
        return (tolerance >= diffPercent);
    }

    /**
     * parse and map the pages and its page timings
     *
     * @param data json data
     * @return json map
     */
   
	private static JSONObject getPageMap(String data) {
        JSONObject pageMap = new JSONObject();
        JSONObject ob = (JSONObject) JSONValue.parse(data);
        for (Object tc : ob.keySet()) {
            JSONArray hars = (JSONArray) ((JSONObject) ob.get(tc)).get("har");
            for (Object e : hars) {
                JSONObject har = (JSONObject) ((JSONObject) e).get("har");
                JSONObject page = (JSONObject) ((JSONArray) (((JSONObject) har.get("log")).get("pages"))).get(0);
                Object pagename = ((JSONObject) har.get("config")).get("name");
                if (!pageMap.containsKey(pagename)) {
                    pageMap.put(pagename, new JSONArray());
                }
                JSONObject pageData = (JSONObject) page.get("pageTimings");
                pageData.put("config", har.get("config"));
                ((JSONArray) pageMap.get(pagename)).add(pageData);
            }
        }
        return pageMap;
    }

    /**
     * map the reference page timings and tolerance of all reference pages
     *
     * @requires the project to be set in project settings
     * @return the ref map.
     */
    private static JSONObject getRefPageTimings() {
        JSONObject ref = new JSONObject();
//        for (Object o : HarCompareHandler.getRefHars()) {
//            JSONObject refReq = (JSONObject) o;
//            JSONObject refHar = HarCompareHandler.getRefData(refReq);
//            JSONObject page = (JSONObject) ((JSONArray) (((JSONObject) refHar.get("log")).get("pages"))).get(0);
//            JSONObject refdata = (JSONObject) page.get("pageTimings");
//            JSONObject config = (JSONObject) refHar.get("config");
//            if (config.containsKey("tolerance")) {
//                refdata.put("tolerance", config.get("tolerance"));
//            }
//            ref.put(refReq.get("name"), refdata);
//        }
        return ref;
    }

}
