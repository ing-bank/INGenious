
package com.ing.engine.reporting.sync.sapi;

import com.ing.engine.reporting.sync.BasicHttpClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;

/**
 *
 * 
 */
public class SAPIClient extends BasicHttpClient {

    Header auth;

    public SAPIClient(String url, String userName, String password) throws MalformedURLException {
        super(new URL(url), userName, password);
    }

    public SAPIClient(Map<String, String> op) throws MalformedURLException {
        this(op.get("api.status.link"), "", "");
        auth = new BasicHeader(HttpHeaders.AUTHORIZATION,
                op.get("api.status.auth").replaceAll(",$", ""));
    }

    @Override
    public void auth(HttpRequest req) {
        req.addHeader(auth);
    }

    
    public boolean hasProxy() {
        return false;
    }

}
