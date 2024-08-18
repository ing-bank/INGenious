
package com.ing.engine.reporting.sync;

import com.ing.engine.support.DLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
//import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
//import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

abstract public class AbstractHttpClient {

	private static final Logger LOG = Logger.getLogger(AbstractHttpClient.class.getName());

	protected CloseableHttpClient client;

	public HttpResponse doGet(HttpGet httpGet) throws Exception {

		// return executeoctane(httpGet);
		return execute(httpGet);
	}

	public HttpResponse doPost(HttpPost httpPost) throws Exception {
		return execute(httpPost);
	}

	public HttpResponse doPut(HttpPut httpPut) throws Exception {
		return execute(httpPut);
	}

	public HttpResponse doPatch(HttpPatch httpPatch) throws Exception {
		return execute(httpPatch);
	}

	/**
	 * execute the given URI request
	 *
	 * @param req
	 * @return
	 * @throws Exception
	 */
	abstract public CloseableHttpResponse execute(HttpUriRequest req) throws Exception;

	public final HttpContext createContext(URI uri, UsernamePasswordCredentials creds) throws Exception {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()), creds);
		org.apache.http.HttpHost host = new org.apache.http.HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);
		HttpClientContext context1 = HttpClientContext.create();
		context1.setCredentialsProvider(credsProvider);
		context1.setAuthCache(authCache);
		return context1;
	}

	/**
	 * returns systen Def client
	 *
	 * @return
	 */
	public final CloseableHttpClient getSystemClient() {
		return HttpClients.createSystem();
	}

	/**
	 * custom http client for server with SSL errors
	 *
	 * @return
	 */
	public final CloseableHttpClient getCustomClient() {
		try {
		/*	HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties();
			SSLContext sslContext = new SSLContextBuilder()
					.loadTrustMaterial(null, (TrustStrategy) (X509Certificate[] arg0, String arg1) -> true).build();
			builder.setSSLContext(sslContext);
			HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", sslSocketFactory).build();
			PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			builder.setConnectionManager(connMgr);
			return builder.build();*/
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, ex.getMessage(), ex);
		}
		return getSystemClient();
	}

	/**
	 * Parse http response as JSON
	 *
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public JSONObject parseResponse(HttpResponse response) throws Exception {

		HttpEntity entity = response.getEntity();
		String resp = "";
		try {
			if (entity != null) {
				resp = EntityUtils.toString(entity);
				JSONParser parser = new JSONParser();
				Object data = parser.parse(resp);
				JSONObject jobj;
				if (data instanceof JSONObject) {
					jobj = (JSONObject) data;
				} else {
					jobj = new JSONObject();
					jobj.put("array", (JSONArray) data);
				}
				EntityUtils.consume(entity);
				return jobj;
			} else {
				return null;
			}
		} catch (Exception ex) {
			DLogger.Log("Unknown Response : ", resp);
			LOG.log(Level.SEVERE, ex.getMessage(), ex);
			return null;
		}
	}

	

	/**
	 * Builds URL params from input JSON string
	 *
	 * @param builder
	 * @param jsonStr
	 * @return
	 * @throws ParseException
	 */
	public URIBuilder setParams(URIBuilder builder, String jsonStr) throws ParseException {

		if (jsonStr != null && !"".equals(jsonStr)) {
			try {
				JSONParser parser = new JSONParser();
				JSONObject json = (JSONObject) parser.parse(jsonStr);
				json.keySet().forEach((Key) -> {
					builder.setParameter(Key.toString(), (String) json.get(Key));
				});
			} catch (Exception ex) {
				DLogger.LogE(ex.getMessage());
				LOG.log(Level.SEVERE, ex.getMessage(), ex);
			}

		}

		return builder;
	}

}
