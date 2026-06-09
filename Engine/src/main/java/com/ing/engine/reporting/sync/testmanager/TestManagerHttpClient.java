package com.ing.engine.reporting.sync.testmanager;

import com.ing.engine.reporting.sync.BasicHttpClient;
import java.net.URL;
import java.util.Map;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;

public class TestManagerHttpClient extends BasicHttpClient {
    private final String authHeader;

    public TestManagerHttpClient(URL url, String username, String apiToken, Map config) {
        super(url, "", "", config);
        // Default: HTTP Basic with username + token. Swap to "Bearer " + apiToken if the
        // Test Manager API requires bearer tokens instead.
        String raw = (username == null ? "" : username) + ":" + (apiToken == null ? "" : apiToken);
        this.authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString(raw.getBytes());
    }

    @Override
    public void auth(HttpRequest req) throws AuthenticationException {
        // No-op: header-based auth applied per-request via setHeader().
    }

    @Override
    public void setHeader(HttpGet httpget) {
        httpget.setHeader("Authorization", authHeader);
        httpget.setHeader("Accept", "application/json");
    }

    @Override
    public void setHeader(HttpPost httppost) {
        httppost.setHeader("Authorization", authHeader);
        httppost.setHeader("Content-Type", "application/json");
        httppost.setHeader("Accept", "application/json");
    }

    @Override
    public void setHeader(HttpPatch httppatch) {
        httppatch.setHeader("Authorization", authHeader);
        httppatch.setHeader("Content-Type", "application/json");
        httppatch.setHeader("Accept", "application/json");
    }

    @Override
    public void setHeader(HttpPut httpput) {
        httpput.setHeader("Authorization", authHeader);
        httpput.setHeader("Content-Type", "application/json");
        httpput.setHeader("Accept", "application/json");
    }
}
