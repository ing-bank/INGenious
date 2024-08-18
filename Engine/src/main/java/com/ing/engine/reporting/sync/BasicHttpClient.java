
package com.ing.engine.reporting.sync;

import com.ing.engine.support.DLogger;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
//import org.apache.http.entity.mime.MultipartEntity;
//import org.apache.http.entity.mime.MultipartEntityBuilder;
//import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.json.simple.JSONObject;

public class BasicHttpClient extends AbstractHttpClient {

    private static final Logger LOG = Logger.getLogger(BasicHttpClient.class.getName());

    public final URL url;
    private CloseableHttpClient client;
    private HttpContext context;
    /**
     * false - if the server has untrusted SSL (accept all cert) true - for
     * default system keystore
     */
    private boolean trusted = false;
    private UsernamePasswordCredentials creds;
    private HttpHost proxy;

    public BasicHttpClient(URL url, String userName, String password) {
        this(url, userName, password, null);
    }

    public BasicHttpClient(URL url, String userName, String password, Map<String, String> config) {
        this.url = url;
        try {
            client = trusted ? getSystemClient() : getCustomClient();
            creds = new UsernamePasswordCredentials(userName, password);
            context = createContext(url.toURI(), creds);
            if (config != null && Boolean.valueOf(config.get("useProxy"))) {
                this.proxy = new HttpHost(config.get("proxyHost"),
                        Integer.valueOf(config.get("proxyPort")));
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error creating HttpClient!", ex);
        }
    }

    /**
     * basic auth implementation
     *
     * @param req
     * @throws AuthenticationException
     */
    public void auth(HttpRequest req) throws AuthenticationException {
        req.addHeader(new BasicScheme().authenticate(creds, req, context));
    }

    /**
     * execute the given URI request
     *
     * @param req
     * @return
     * @throws Exception
     */
    @Override
    public CloseableHttpResponse execute(HttpUriRequest req) throws Exception {
        DLogger.Log(req.toString());
        Optional.ofNullable(proxy).ifPresent((p)
                -> ((HttpRequestBase) req).setConfig(RequestConfig.custom().setProxy(p).build()));
        return client.execute(req, context);
    }

// <editor-fold defaultstate="collapsed" desc="PUT implementation">
    /**
     * Http Post request for given data as JSON string
     *
     * @param targetUrl
     * @param data
     * @return
     * @throws Exception
     */
    public JSONObject put(URL targetUrl, String data) throws Exception {
        HttpPut httpput = new HttpPut(targetUrl.toURI());
        setPutEntity(data, httpput);
        auth(httpput);
        setHeader(httpput);
        return parseResponse(doPut(httpput));
    }
    
    public JSONObject put(URL targetUrl, String data, String accessKey, String value) throws Exception {
        HttpPut httpput = new HttpPut(targetUrl.toURI());
        setPutEntity(data, httpput);
        auth(httpput);
        setHeader(httpput);
        addHeader(httpput, accessKey, value);
        return parseResponse(doPut(httpput));
    }
    

    private void addHeader(HttpPut httpput, String accessKey, String value) {
    	httpput.addHeader(accessKey, value);
	}

	/**
     * custom header for respective client
     *
     * @param httpput
     */
    public void setHeader(HttpPut httpput) {
        httpput.addHeader("Accept", "application/json");
    }
    
    public void addHeader(HttpGet httpGet, String Key, String Value) {
        httpGet.addHeader(Key, Value);
    }

    public void setPutEntity(String xmlstr, HttpPut httpput) throws UnsupportedEncodingException {
        StringEntity input = new StringEntity(xmlstr);
        if (xmlstr != null && !xmlstr.isEmpty()) {
            input.setContentType("application/json");
        }
        httpput.setEntity(input);
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="POST implementation">
    /**
     * Http Post request for given data as JSON string
     *
     * @param targetUrl
     * @param payload
     * @return
     * @throws Exception
     */
    public JSONObject post(URL targetUrl, String payload) throws Exception {
        HttpPost httppost = new HttpPost(targetUrl.toURI());
        setPostEntity(payload, httppost);
        return parseResponse(doPost(httppost));
    }
    
    /**
     * Http Post request for given data as JSON string
     *
     * @param targetUrl
     * @param payload
     * @return
     * @throws Exception
     */
    public JSONObject post(URL targetUrl, File toUplod, String key, String value) throws Exception {
        HttpPost httppost = new HttpPost(targetUrl.toURI());
     //   setPostEntityJ(toUplod, httppost);
        return parseResponse(doPost(httppost, key, value));
    }

    /**
     * Http Post request for uploading files
     *
     * @param targetUrl
     * @param toUplod
     * @return
     * @throws Exception
     */
    public JSONObject post(URL targetUrl, File toUplod) throws Exception {
        HttpPost httppost = new HttpPost(targetUrl.toURI());
     //   setPostEntity(toUplod, httppost);
        return parseResponse(doPost(httppost));
    }
    
    public JSONObject put(URL targetUrl, File toUplod) throws Exception {
        HttpPut httpput = new HttpPut(targetUrl.toURI());
     //   setPutEntity(toUplod, httpput);
        return parseResponse(doPut(httpput));
    }

    public JSONObject post(URL targetUrl, List<NameValuePair> parameters) throws Exception {
        HttpPost httppost = new HttpPost(targetUrl.toURI());
        setPostEntity(parameters, httppost);
        return parseResponse(doPost(httppost));
    }

    /**
     * Http Post request for uploading files
     *
     * @param targetUrl
     * @param data
     * @param toUplod
     * @return
     * @throws Exception
     */
    public JSONObject post(URL targetUrl, String data, File toUplod) throws Exception {
        HttpPost httppost = new HttpPost(targetUrl.toURI());
     //   setPostEntity(data, toUplod, httppost);
        return parseResponse(doPost(httppost));
    }

    /**
     * custom header for respective client
     *
     * @param httppost
     */
    public void setHeader(HttpPost httppost) {
        httppost.addHeader("Accept", "application/json");
    }

    @Override
    public HttpResponse doPost(HttpPost httpPost) throws Exception {
        auth(httpPost);
        setHeader(httpPost);
        return super.doPost(httpPost);
    }
    
   public HttpResponse doPost(HttpPost httpPost, String key, String value) throws Exception {
        auth(httpPost);
        setHeader(httpPost);
        addHeader(httpPost, key, value);
        return super.doPost(httpPost);
    }

    private void addHeader(HttpPost httpPost, String key, String value) {
    	httpPost.addHeader(key, value);
    }

//	public void setPostEntity(File toUplod, HttpPost httppost) {
//        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        builder.addBinaryBody("file", toUplod,
//                ContentType.APPLICATION_OCTET_STREAM, toUplod.getName());
//        httppost.setEntity(builder.build());
//    }
	
//	public void setPutEntity(File toUplod, HttpPut httpput) {
//        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        builder.addBinaryBody("file", toUplod,
//                ContentType.APPLICATION_OCTET_STREAM, toUplod.getName());
//        httpput.setEntity(builder.build());
//    }
	
//	public void setPostEntityJ(File toUplod, HttpPost httppost) {
//		MultipartEntity entity = new MultipartEntity();
//		entity.addPart("attachment", new FileBody(toUplod));
//		httppost.setEntity(entity);
//    }

//    public void setPostEntity(String data, File file, HttpPost httppost) {
//        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
//        builder.addTextBody("body", data, ContentType.APPLICATION_XML);
//        httppost.setEntity(builder.build());
//    }

    public void setPostEntity(String jsonStr, HttpPost httppost) throws UnsupportedEncodingException {
        StringEntity input = new StringEntity(jsonStr);
        input.setContentType("application/json");
        httppost.addHeader("accept", "application/json");
        httppost.setEntity(input);
    }

    public void setPostEntity(List<NameValuePair> params, HttpPost httppost) {
        try {
            final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
            httppost.setEntity(entity);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(BasicHttpClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="PATCH implementation">
    public JSONObject patch(URL targetUrl, String payload) throws Exception {
        HttpPatch httppatch = new HttpPatch(targetUrl.toURI());
        auth(httppatch);
        setHeader(httppatch);
        setPatchEntity(payload, httppatch);
        return parseResponse(doPatch(httppatch));
    }

    /**
     * custom header for respective client
     *
     * @param httppatch
     */
    public void setHeader(HttpPatch httppatch) {
        httppatch.addHeader("Accept", "application/json");
    }

    public void setPatchEntity(String jsonStr, HttpPatch httppatch) throws UnsupportedEncodingException {
        StringEntity input = new StringEntity(jsonStr);
        input.setContentType("application/json");
        httppatch.setEntity(input);
    }
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="GET implementation">
    /**
     * Http Get request for given url
     *
     * @param targetUrl
     * @return
     * @throws Exception
     */
    public JSONObject Get(URL targetUrl) throws Exception {
        return Get(targetUrl.toURI());
    }

    public JSONObject Get(URL targetUrl, String key, String val) throws Exception {
        URIBuilder builder = new URIBuilder(targetUrl.toString());
        builder.setParameter(key, val);
        return Get(builder.build());
    }
    
    public JSONObject Get(URL targetUrl, String key, String val, String empty) throws Exception {
        URIBuilder builder = new URIBuilder(targetUrl.toString());
        builder.setParameter(key, val);
        return Get(builder.build(), key, val);
    }
    
    public JSONObject Get(URL targetUrl, boolean isJwtToken, String key, String val ) throws Exception {
        URIBuilder builder = new URIBuilder(targetUrl.toString());
        return Get(builder.build(), key, val);
    }
    
   
    

    /**
     * Http Get request for given params as JSON string
     *
     * @param targetUrl
     * @param jsonStr
     * @return
     * @throws Exception
     */
    public JSONObject Get(URL targetUrl, String jsonStr) throws Exception {
        URIBuilder builder = new URIBuilder(targetUrl.toString());
         setParams(builder, jsonStr);
        return Get(setParams(builder, jsonStr).build());
    }

    /**
     * custom header for respective client
     *
     * @param httpGet
     */
    public void setHeader(HttpGet httpGet) {
        httpGet.addHeader("Accept", "application/json");
    }

    private JSONObject Get(URI uri) throws Exception {
        HttpGet httpGet = new HttpGet(uri);
        auth(httpGet);
        setHeader(httpGet);
        return parseResponse(doGet(httpGet));
    }
    
    private JSONObject Get(URI uri, String Key, String Value) throws Exception {
        HttpGet httpGet = new HttpGet(uri);
        auth(httpGet);
        setHeader(httpGet);
        addHeader(httpGet, Key, Value);
        return parseResponse(doGet(httpGet));
    }
    
// </editor-fold>

}
