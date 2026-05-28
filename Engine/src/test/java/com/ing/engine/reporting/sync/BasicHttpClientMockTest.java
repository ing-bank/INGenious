package com.ing.engine.reporting.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for BasicHttpClient — focuses on entity setting, header config,
 * and construction with proxy configuration.
 * Replaces the disabled tests in BasicHttpClientTest.
 */
public class BasicHttpClientMockTest {

    private URL testUrl;
    private BasicHttpClient client;

    @BeforeMethod
    public void setUp() throws Exception {
        testUrl = new URL("http://localhost:8080/api");
        client = new BasicHttpClient(testUrl, "user", "pass");
    }

    // ---- Construction ----

    @Test
    public void testConstructorSetsUrl() {
        assertThat(client.url).isEqualTo(testUrl);
    }

    @Test
    public void testConstructorWithProxy() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("useProxy", "true");
        config.put("proxyHost", "proxy.example.com");
        config.put("proxyPort", "3128");

        BasicHttpClient proxyClient = new BasicHttpClient(testUrl, "user", "pass", config);
        assertThat(proxyClient.url).isEqualTo(testUrl);
    }

    @Test
    public void testConstructorWithoutProxy() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("useProxy", "false");

        BasicHttpClient noProxyClient = new BasicHttpClient(testUrl, "user", "pass", config);
        assertThat(noProxyClient.url).isEqualTo(testUrl);
    }

    @Test
    public void testConstructorNullConfig() throws Exception {
        BasicHttpClient nullConfigClient = new BasicHttpClient(testUrl, "user", "pass", null);
        assertThat(nullConfigClient.url).isEqualTo(testUrl);
    }

    // ---- setHeader methods ----

    @Test
    public void testSetHeaderPost() {
        HttpPost httppost = new HttpPost("http://localhost/test");
        client.setHeader(httppost);
        assertThat(httppost.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    @Test
    public void testSetHeaderPut() {
        HttpPut httpput = new HttpPut("http://localhost/test");
        client.setHeader(httpput);
        assertThat(httpput.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    @Test
    public void testSetHeaderPatch() {
        HttpPatch httppatch = new HttpPatch("http://localhost/test");
        client.setHeader(httppatch);
        assertThat(httppatch.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    @Test
    public void testSetHeaderGet() {
        HttpGet httpget = new HttpGet("http://localhost/test");
        client.setHeader(httpget);
        assertThat(httpget.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    // ---- setPostEntity ----

    @Test
    public void testSetPostEntityJsonSetsContentType() throws UnsupportedEncodingException {
        HttpPost httppost = new HttpPost("http://localhost/test");
        client.setPostEntity("{\"key\":\"value\"}", httppost);
        assertThat(httppost.getEntity()).isNotNull();
        assertThat(httppost.getEntity().getContentType().getValue()).contains("application/json");
    }

    @Test
    public void testSetPostEntityNameValuePairs() {
        HttpPost httppost = new HttpPost("http://localhost/test");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("key1", "val1"));
        params.add(new BasicNameValuePair("key2", "val2"));
        client.setPostEntity(params, httppost);
        assertThat(httppost.getEntity()).isNotNull();
    }

    // ---- setPutEntity ----

    @Test
    public void testSetPutEntityJsonSetsContentType() throws UnsupportedEncodingException {
        HttpPut httpput = new HttpPut("http://localhost/test");
        client.setPutEntity("{\"key\":\"value\"}", httpput);
        assertThat(httpput.getEntity()).isNotNull();
        assertThat(httpput.getEntity().getContentType().getValue()).contains("application/json");
    }

    @Test
    public void testSetPutEntityEmptyString() throws UnsupportedEncodingException {
        HttpPut httpput = new HttpPut("http://localhost/test");
        client.setPutEntity("", httpput);
        assertThat(httpput.getEntity()).isNotNull();
    }

    // ---- setPatchEntity ----

    @Test
    public void testSetPatchEntityJsonSetsContentType() throws UnsupportedEncodingException {
        HttpPatch httppatch = new HttpPatch("http://localhost/test");
        client.setPatchEntity("{\"key\":\"value\"}", httppatch);
        assertThat(httppatch.getEntity()).isNotNull();
        assertThat(httppatch.getEntity().getContentType().getValue()).contains("application/json");
    }

    // ---- addHeader ----

    @Test
    public void testAddHeaderGet() {
        HttpGet httpget = new HttpGet("http://localhost/test");
        client.addHeader(httpget, "Authorization", "Bearer token123");
        assertThat(httpget.getFirstHeader("Authorization").getValue()).isEqualTo("Bearer token123");
    }

    // ---- auth ----

    @Test
    public void testAuthAddsAuthorizationHeader() throws Exception {
        HttpGet httpget = new HttpGet("http://localhost/test");
        client.auth(httpget);
        assertThat(httpget.getFirstHeader("Authorization")).isNotNull();
        assertThat(httpget.getFirstHeader("Authorization").getValue()).startsWith("Basic ");
    }
}
