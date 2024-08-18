
package com.ing.engine.reporting.sync.azure;

import com.ing.engine.reporting.sync.BasicHttpClient;
import java.net.URL;
import java.util.Map;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;

public class AzureHttpClient extends BasicHttpClient {

    final String ACCESSTOKEN;

    final String encodedToken;

    public AzureHttpClient(URL urL, String PAT,Map config) {
        super(urL, "", "",config);
        ACCESSTOKEN = PAT;
        encodedToken = java.util.Base64.getEncoder().encodeToString((":"+ACCESSTOKEN).getBytes());
    }

    @Override
    public void auth(HttpRequest req) throws AuthenticationException {
    }

    @Override
    public void setHeader(HttpGet httpget) {
        httpget.setHeader("Authorization", "Basic " + encodedToken);
        httpget.setHeader("Accept", "application/json");
    }

    @Override
    public void setHeader(HttpPost httppost) {
        httppost.setHeader("Authorization", "Basic " + encodedToken);
        httppost.setHeader("Content-Type", "application/json");
    }

    @Override
    public void setHeader(HttpPatch httppatch) {
        httppatch.setHeader("Authorization", "Basic " + encodedToken);
        httppatch.setHeader("Content-Type", "application/json");
    }

}
