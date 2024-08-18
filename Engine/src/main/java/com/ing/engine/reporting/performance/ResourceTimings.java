
package com.ing.engine.reporting.performance;

import com.ing.engine.reporting.performance.har.MIME;

/**
 * Resource Timings api
 *
 * 
 */
public class ResourceTimings {

    public Double connectEnd,
            connectStart,
            domainLookupEnd,
            domainLookupStart,
            duration,
            fetchStart,
            redirectEnd,
            redirectStart,
            requestStart,
            responseEnd,
            responseStart,
            secureConnectionStart,
            startTime;
    public String entryType,
            initiatorType,
            name;

    /**
     * initial adjustment makes sure timings in the desired order
     */
    public void adjust() {

        fetchStart = Math.max(fetchStart, startTime);
        connectStart = Math.max(fetchStart, connectStart);
        redirectStart = Math.max(redirectStart, startTime);
        responseEnd = Math.max(redirectStart, responseEnd);
        domainLookupStart = Math.max(domainLookupStart, fetchStart);
        domainLookupEnd = Math.max(domainLookupStart, domainLookupEnd);
        secureConnectionStart = Math.max(secureConnectionStart, connectStart);
        connectEnd = Math.max(secureConnectionStart, connectEnd);
        requestStart = Math.max(requestStart, connectEnd);
        responseStart = Math.max(requestStart, responseStart);

    }

    public String mimeType() {

        String mime = com.ing.engine.util.data.mime.MIME.getType(name);
        if (mime != null && !mime.isEmpty()) {
            return mime;
        }
        if ("script".equalsIgnoreCase(initiatorType) || name.endsWith(".js")) {
            return MIME.JS.val();
        } else if ("image".equalsIgnoreCase(initiatorType)) {
            return "image/" + (name.contains(".") ?
                    name.substring(name.lastIndexOf(".") + 1) : initiatorType);
        }
        return this.initiatorType;
    }

    /**
     * java script to extract resource timings
     *
     * @return
     */
    public static String script() {
        return "var dmp=window.performance.getEntriesByType('resource');"
                + "var resources=[];"
                + "for(var r in dmp){"
                + "var resource={};"
                + "for(var k in dmp[r]){resource[k]=dmp[r][k];}"
                + "resource.toJSON=undefined;"
                + "resources.push(resource);}"
                + "JSON.stringify(resources);";
    }

}
