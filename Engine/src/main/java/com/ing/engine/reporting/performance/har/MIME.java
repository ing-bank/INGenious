
package com.ing.engine.reporting.performance.har;

/**
 * MIME helper class for har
 *
 * 
 * 
 */
public enum MIME {

    HTML("text/html", ".html"),
    PNG("image/png", ".png"),
    JS("application/javascript", ".js"),
    CSS("text/css", ".css");
    /**
     * need to be added
     */
    public String val, ext;

    private MIME(String mime, String ext) {
        this.val = mime;
        this.ext = ext;
    }

    public String val() {
        return val;
    }

}
