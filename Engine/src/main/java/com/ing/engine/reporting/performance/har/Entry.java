
package com.ing.engine.reporting.performance.har;

import com.ing.engine.reporting.performance.PerformanceTimings;
import com.ing.engine.reporting.performance.ResourceTimings;
import com.google.gson.Gson;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * helper class for har resource entry
 *
 * 
 * 
 * @see
 * https://dvcs.w3.org/hg/webperf/raw-file/tip/specs/HAR/Overview.html#sec-object-types-entries
 */
@SuppressWarnings("unchecked")
public final class Entry extends JSONObject {

    private static final Logger LOG = Logger.getLogger(com.ing.engine.reporting.performance.har.Entry.class.getName());
    private static final long serialVersionUID = 1L;
    private static final String DF = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    ResourceTimings e;
    Request req;
    Response res;
    Timings t;

    public Entry(String resourceTimings, Page p) {
        super();
        Gson gson = new Gson();
        e = gson.fromJson(resourceTimings, ResourceTimings.class);
        create(p);
    }
   
    public Entry(Page p) {
        super();
        e = p.pt.toResourceTimings();
        create(p);
    }

    protected void create(Page p1) {
        try {
            put("startedDateTime", getMillstoDate(Math.round(p1.pt.navigationStart + e.startTime)));
            put("pageref", p1.getID());
            put("cache", Prop.getEmpty());
            req = new Request(e);
            put("request", req);
            res = new Response(e);
            put("response", res);
            t = new Timings(p1.pt, e);
            put("timings", t);
            put("time", processed(t.duration));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    protected long processed(Double v) {
        return Math.round(v);
    }

    public static String getMillstoDate(long mills) {
        SimpleDateFormat df = new SimpleDateFormat(DF);
        return df.format(new Date(mills));
    }

    public void addDef(JSONObject obj, boolean isReq) {
        obj.put("httpVersion", "HTTP/1.x");
        obj.put("headers", getHeaders(isReq));
        obj.put("cookies", Prop.getEmptyArray());
        obj.put("headersSize", -1);
        obj.put("bodySize", -1);
    }

    
	public JSONArray getHeaders(boolean isReq) {

        JSONArray hs = Prop.getEmptyArray();
        if (!isReq) {
            hs.add(getHeader("Content-Type", e.mimeType()));
        }
        return hs;
    }

    public JSONObject getHeader(String name, String value) {
        JSONObject h = new JSONObject();
        h.put("name", name);
        h.put("value", value);
        return h;
    }

    final class Request extends JSONObject {

        private static final long serialVersionUID = 1L;

        
        public Request(ResourceTimings e) {
            put("method", "GET");
            put("url", e.name);
            addDef((JSONObject)this, true);
            put("queryString", getParams(e));
        }

        public JSONArray getParams(ResourceTimings e) {
            JSONArray paramList = new JSONArray();
            try {
                List<NameValuePair> params = URLEncodedUtils.parse(new URI(e.name), "UTF-8");
                for (NameValuePair pair : params) {
                    JSONObject jsonPair = new JSONObject();
                    jsonPair.put("name", pair.getName());
                    jsonPair.put("value", pair.getValue());
                    paramList.add(jsonPair);
                }

            } catch (Exception ex) {
                return paramList;
            }
            return paramList;
        }
    }

    class Response extends JSONObject {

        private static final long serialVersionUID = 1L;

        
        private Response(ResourceTimings e) {
            put("status", 200);
            put("statusText", "OK");
            put("_transferSize", -1);
            put("redirectURL", "");
            put("content", new Content(e));
            addDef((JSONObject)this, false);
        }

        class Content extends JSONObject {

            private static final long serialVersionUID = 1L;

            /**
             * size not supported (by browsers) as of Dec 2015 (in draft)
             *
             * @param e
             */
           
            public Content(ResourceTimings e) {
                put("size", 0);
                put("mimeType", e.mimeType());
                put("compression", 0);
            }
        }

    }

    final class Timings extends JSONObject {

        private static final long serialVersionUID = 1L;
        public Double duration;

       
        public Timings(PerformanceTimings pt, ResourceTimings e) {
            Integer na = -1;
            Double blocked = Math.max(0, e.connectStart - e.startTime);
            
            Double dns = (e.domainLookupEnd - e.domainLookupStart) == 0 ? na
                    : (e.domainLookupEnd - e.domainLookupStart);

            Double connect = (e.connectEnd - e.connectStart) == 0 ? na
                    : (e.connectEnd - e.connectStart);

            Double send = Math.max(0, e.responseStart - e.requestStart);

            Double receive = Math.max(0, e.responseEnd - e.responseStart);
            Double ssl = -1d;
            try {
                // calc ssl only if req is secure connection
                if (e.name != null && e.name.toLowerCase().startsWith("https:")) {
                    ssl = Math.round(e.secureConnectionStart) == 0 ? na
                            : (e.connectEnd - e.secureConnectionStart);
                }
            } catch (Exception ex) {
            }
            Double time = e.duration;
            //calculate wait time from time unaccunted (avoids bizzare graph)
            long wait = rnd(time) - rnd(nonNtve(dns)) - rnd(nonNtve(connect)) - rnd(nonNtve(ssl))
                    - rnd(send) - rnd(receive) - rnd(blocked);
            if (wait < 0) {
                time -= wait;
                wait = 0;
            }

            duration = time;

            put("blocked", rnd(blocked));
            put("dns", rnd(dns));
            put("connect", rnd(connect));
            put("send", rnd(send));
            put("wait", wait);
            put("receive", rnd(receive));
            put("ssl", rnd(ssl));

        }

        protected double nonNtve(Double d) {
            return Math.max(0, d);
        }

        protected long rnd(Double v) {
            return Math.round(v);
        }
    }
}
