
package com.ing.engine.reporting.sync;

import com.ing.engine.reporting.sync.BasicHttpClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;
import static org.testng.Assert.*;
import org.testng.annotations.*;

public class BasicHttpClientTest {

    private JSONObject getArgs;
    private static final int PORT = 3210;
    
    public BasicHttpClientTest() throws Exception {

    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
        getArgs = new JSONObject();
        getArgs.put("data", "vola");
        getArgs.put("empty", "");
        getArgs.put("special", "^\\d{5,}$");
    }

    /**
     * Test of Get method, of class BasicHttpClient.
     * @throws java.lang.Exception
     */
    @Test(enabled = false,description = "http-get of remote address")
    public void testGetHttp() throws Exception {
        System.out.println("Get-http");
        URL targetUrl = new URL("http://postman-echo.com/get");
        BasicHttpClient instance = new BasicHttpClient(targetUrl, "anon", "anon");        
        JSONObject result = instance.Get(targetUrl, getArgs.toJSONString());
        assertEquals(result.get("args"), getArgs);
    }

    /**
     * Test of Get method, of class BasicHttpClient.
     * @throws java.lang.Exception
     */
    @Test(enabled = false,description = "https-get of remote address")
    public void testGetHttps() throws Exception {
        System.out.println("Get-https");
        URL targetUrl = new URL("https://postman-echo.com/get");
        BasicHttpClient instance = new BasicHttpClient(targetUrl, "anon", "anon");
        JSONObject result = instance.Get(targetUrl, getArgs.toJSONString());
        assertEquals(result.get("args"), getArgs);
    }

    /**
     * Test of Get method, of class BasicHttpClient.
     * @throws java.lang.Exception
     */
    @Test(enabled = false,description = "http-get of local address")
    public void testGetHttpLocal() throws Exception {
        System.out.println("Get-http-local");
        URL targetUrl = new URL("http://127.0.0.1:" + PORT);
        BasicHttpClient instance = new BasicHttpClient(targetUrl, "anon", "anon");        
        JSONObject result = instance.Get(targetUrl, "data", "vola");
        assertEquals(result.toString(), "{\"data\":\"vola\"}");
    }

}
