package com.ing.engine.commands.webservice;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.ingenious.api.contract.WebservicePluginApi;
import com.ing.ingenious.api.exception.ActionException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.RequestMethodType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class GeneralWebservice extends Command implements WebservicePluginApi {

    public GeneralWebservice(CommandControl cc) {
        super(cc);
    }

    protected TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
    };

    /**
     * Implementation of WebservicePluginApi.createHttpRequest() for the API-plugin contract.
     * Creates and executes an HTTP request with the specified request method.
     * @param requestmethod the HTTP request method to use
     */
    @Override
    public void createHttpRequest(RequestMethodType requestmethod)
        throws InterruptedException, Exception {
        try {
            setheaders();
            setRequestMethod(requestmethod);
            before.put(key, Instant.now());

            returnResponseDetails();
            duration.put(key, Duration.between(before.get(key), after.get(key)).toMillis());
            Report.updateTestLog(
                Action,
                "Response received in : [" +
                duration.get(key) +
                "ms] with Status code  : " +
                responsecodes.get(key),
                Status.COMPLETE
            );

            if (headers.containsKey(key)) {
                if (!headers.get(key).isEmpty()) {
                    headers.get(key).clear();
                }
            }
        } catch (IOException ex) {
            int responseCode = 0;
            Matcher exMsgStatusCodeMatcher = Pattern
                .compile("^Server returned HTTP response code: (\\d+)")
                .matcher(ex.getMessage());

            if (exMsgStatusCodeMatcher.find()) {
                responseCode = Integer.parseInt(exMsgStatusCodeMatcher.group(1));
            } else if (ex.getClass().getSimpleName().equals("FileNotFoundException")) {
                System.out.println(
                    "\n =====================================\n" +
                    " Returned [FileNotFoundException]" +
                    "\n =====================================\n"
                );
                responseCode = 404;
            } else {
                System.out.println(
                    "Exception (" +
                    ex.getClass().getSimpleName() +
                    ") doesn't contain status code: " +
                    ex
                );
            }
            if (responseCode == 0) {
                System.out.println(
                    "\n =====================================\n" +
                    "Response Code does not exist in Exception" +
                    "\n =====================================\n"
                );
            } else {
                responsecodes.put(key, Integer.toString(responseCode));
            }

            if (
                responseCode == 400 ||
                responseCode == 401 ||
                responseCode == 402 ||
                responseCode == 403 ||
                responseCode == 404
            ) {
                Report.updateTestLog(
                    Action,
                    "Error in executing [" +
                    requestmethod.toString() +
                    "] request : " +
                    "\n" +
                    ex.getMessage(),
                    Status.DONE
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Error in executing " +
                    requestmethod.toString() +
                    " request : " +
                    "\n" +
                    ex.getMessage(),
                    Status.DEBUG
                );
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new ActionException(e);
        }
    }

    protected void setRequestMethod(RequestMethodType requestmethod)
        throws FileNotFoundException, IOException {
        if (
            requestmethod.toString().equals("PUT") ||
            requestmethod.toString().equals("POST") ||
            requestmethod.toString().equals("PATCH") ||
            requestmethod.toString().equals("DELETEWITHPAYLOAD")
        ) {
            setRequestMethod(requestmethod.toString(), handlePayloadorEndpoint(Data));
        } else {
            setRequestMethod(requestmethod.toString(), "");
        }
    }

    protected void setRequestMethod(String method, String payload) throws IOException {
        BodyPublisher payloadBody = null;
        if (isformUrlencoded()) {
            payload = urlencodedParams();
        }
        if (isMultiPart()) {
            Path filePath = Path.of(getVar("%filePath%"));
            filePath = Path.of(Control.getCurrentProject().getLocation() + "/" + filePath);
            String mimeType = Files.probeContentType(filePath);
            System.out.println("Path of the file === " + filePath);
            String boundary = "Boundary-" + System.currentTimeMillis();
            String fileName = filePath.getFileName().toString();

            var byteArrays = new ArrayList<byte[]>();
            byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(
                (
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" +
                    fileName +
                    "\"\r\n"
                ).getBytes(StandardCharsets.UTF_8)
            );
            byteArrays.add(
                ("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8)
            );
            byteArrays.add(Files.readAllBytes(filePath));
            byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            payloadBody = HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
            httpRequestBuilder.put(
                key,
                httpRequestBuilder
                    .get(key)
                    .setHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
            );
        } else {
            payloadBody = HttpRequest.BodyPublishers.ofString(payload);
        }
        try {
            switch (method) {
                case "POST":
                    {
                        httpRequestBuilder.put(key, httpRequestBuilder.get(key).POST(payloadBody));
                        savePayload("request", payload);
                        break;
                    }
                case "PUT":
                    {
                        httpRequestBuilder.put(key, httpRequestBuilder.get(key).PUT(payloadBody));
                        savePayload("request", payload);
                        break;
                    }
                case "PATCH":
                    {
                        httpRequestBuilder.put(
                            key,
                            httpRequestBuilder.get(key).method("PATCH", payloadBody)
                        );
                        savePayload("request", payload);
                        break;
                    }
                case "GET":
                    {
                        httpRequestBuilder.put(key, httpRequestBuilder.get(key).GET());
                        break;
                    }
                case "DELETE":
                    {
                        httpRequestBuilder.put(key, httpRequestBuilder.get(key).DELETE());
                        break;
                    }
                case "DELETEWITHPAYLOAD":
                    {
                        httpRequestBuilder.put(
                            key,
                            httpRequestBuilder.get(key).method("DELETE", payloadBody)
                        );
                        savePayload("request", payload);
                        break;
                    }
            }
            headers.remove(key);
            urlParams.remove(key);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    protected void returnResponseDetails() throws IOException, InterruptedException {
        initiateClientBuilder();
        sslCertificateVerification();
        handleProxy();

        httpClient.put(
            key,
            httpClientBuilder.get(key).followRedirects(getRedirectPolicy()).build()
        );
        httpRequest.put(key, httpRequestBuilder.get(key).build());
        response.put(
            key,
            httpClient.get(key).send(httpRequest.get(key), HttpResponse.BodyHandlers.ofString())
        );

        responsebodies.put(key, (String) response.get(key).body());

        after.put(key, Instant.now());
        savePayload("response", (String) response.get(key).body());

        responsecodes.put(key, Integer.toString(response.get(key).statusCode()));
    }

    /**
     * Retrieves the HTTP redirect policy configured for the current API driver settings.
     * <p>
     * The logic follows three strict rules:
     * <ul>
     *   <li>If no value is configured (i.e., the property is {@code null} or blank), the method defaults to
     *       {@link Redirect#NEVER}.</li>
     *   <li>If a valid redirect policy is provided (one of {@code NEVER}, {@code NORMAL}, or {@code ALWAYS},
     *       case-insensitive), the corresponding {@link Redirect} enum is returned.</li>
     *   <li>If a value is provided but does not match any {@link Redirect} enum constant, the method throws an
     *       {@link IllegalArgumentException} to indicate a configuration error.</li>
     * </ul>
     * </p>
     *
     * @return the resolved {@link Redirect} policy to be applied when building the {@link java.net.http.HttpClient}
     * @throws IllegalArgumentException if a non-blank but invalid redirect value is configured
     */
    private Redirect getRedirectPolicy() {
        String httpClientRedirect = Control
            .getCurrentProject()
            .getProjectSettings()
            .getDriverSettings()
            .getHttpClientRedirect();

        if (httpClientRedirect == null || httpClientRedirect.trim().isEmpty()) {
            return Redirect.NEVER;
        }

        try {
            return Redirect.valueOf(httpClientRedirect.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Invalid httpClientRedirect value: '" +
                httpClientRedirect +
                "'. Allowed values: NEVER, NORMAL, ALWAYS."
            );
        }
    }

    /**
     * Checks if the request is configured for form URL encoding.
     * <p>
     * Examines the headers to determine if the content type is set to
     * application/x-www-form-urlencoded.
     *
     * @return true if form URL encoding is configured, false otherwise
     */
    protected boolean isformUrlencoded() {
        if (headers.containsKey(key)) {
            ArrayList<String> headerlist = headers.get(key);
            if (headerlist.size() > 0) {
                for (String header : headerlist) {
                    if (header.split("=")[1].contains("x-www-form-urlencoded")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Converts URL parameters to URL-encoded string format.
     * <p>
     * Transforms the stored URL parameters into a properly encoded query string
     * suitable for form URL encoding.
     *
     * @return URL-encoded parameter string
     */
    protected String urlencodedParams() {
        Map<String, String> parameters = new HashMap<>();
        String urlParamString = "";
        try {
            ArrayList<String> params = urlParams.get(key);
            for (String param : params) {
                parameters.put(param.split("=", 2)[0], param.split("=", 2)[1]);
            }
            urlParamString =
                parameters
                    .entrySet()
                    .stream()
                    .map(
                        e ->
                            e.getKey() +
                            "=" +
                            URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                    )
                    .collect(Collectors.joining("&"));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
        return urlParamString;
    }

    protected boolean isMultiPart() {
        if (headers.containsKey(key)) {
            ArrayList<String> headerlist = headers.get(key);
            if (headerlist.size() > 0) {
                for (String header : headerlist) {
                    if (header.split("=")[1].contains("multipart")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected void savePayload(String reqOrRes, String data) {
        String payloadFileName = "";
        String path = "";
        if (reqOrRes.equals("request")) {
            payloadFileName = Report.getWebserviceRequestFileName();
        } else if (reqOrRes.equals("response")) {
            payloadFileName = Report.getWebserviceResponseFileName();
        }
        try {
            if (!payloadFileName.isBlank()) {
                path = FilePath.getCurrentResultsPath() + File.separator + "webservice";
                File file = new File(path);
                file.mkdirs();
                File location = new File(FilePath.getCurrentResultsPath() + payloadFileName);
                if (location.createNewFile()) {
                    FileWriter writer = new FileWriter(location);
                    writer.write(data);
                    // Appending headers when saving response
                    if (reqOrRes.equals("response")) {
                        writer.write("\n\n--- Response Headers ---\n");
                        writer.write(response.get(key).headers().toString());
                    }
                    writer.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void initiateClientBuilder() {
        try {
            httpClientBuilder.put(
                key,
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            );
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    protected void sslCertificateVerification() {
        try {
            if (!isSSLCertificateVerification()) {
                SSLContext sc = SSLContext.getInstance("TLS");
                if (isSelfSigned()) {
                    sc.init(loadKeyStore(), trustAllCerts, new SecureRandom());
                } else {
                    sc.init(null, trustAllCerts, new SecureRandom());
                }
                httpClientBuilder.put(key, httpClientBuilder.get(key)).sslContext(sc);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    protected void handleProxy() {
        try {
            if (getProxyDetails() != null) {
                System.out.println(
                    "\nRequest opened with following proxy details :\n" +
                    getProxyDetails().toString() +
                    "\n"
                );
                httpClientBuilder.put(key, httpClientBuilder.get(key).proxy(getProxyDetails()));
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    protected ProxySelector getProxyDetails() {
        if (Control.getCurrentProject().getProjectSettings().getDriverSettings().useProxy()) {
            String proxyhost = Control
                .getCurrentProject()
                .getProjectSettings()
                .getDriverSettings()
                .getProxyHost()
                .replaceFirst("^(http://|https://)", "");
            String proxyport = Control
                .getCurrentProject()
                .getProjectSettings()
                .getDriverSettings()
                .getProxyPort();
            ProxySelector proxySelector = ProxySelector.of(
                new InetSocketAddress(proxyhost, Integer.parseInt(proxyport))
            );
            return proxySelector;
        } else {
            return null;
        }
    }

    protected String getHttpAgentDetails() {
        if (
            Control
                .getCurrentProject()
                .getProjectSettings()
                .getUserDefinedSettings()
                .stringPropertyNames()
                .contains("http.agent")
        ) {
            if (!getUserDefinedData("http.agent").isEmpty()) {
                httpagents.put(key, getUserDefinedData("http.agent"));
                return httpagents.get(key);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected String handlePayloadorEndpoint(String data) throws FileNotFoundException {
        String payloadstring = data;
        payloadstring = handleDataSheetVariables(payloadstring);
        payloadstring = handleuserDefinedVariables(payloadstring);
        System.out.println("Payload :" + payloadstring);
        return payloadstring;
    }

    protected String handleDataSheetVariables(String payloadstring) {
        List<String> sheetlist = Control
            .getCurrentProject()
            .getTestData()
            .getTestDataFor(Control.exe.runEnv())
            .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (payloadstring.contains("{" + sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control
                    .getCurrentProject()
                    .getTestData()
                    .getTestDataByName(sheetlist.get(sheet));
                List<String> columns = tdModel.getColumns();
                for (int col = 0; col < columns.size(); col++) {
                    if (
                        payloadstring.contains(
                            "{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}"
                        )
                    ) {
                        payloadstring =
                            payloadstring.replace(
                                "{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                userData.getData(sheetlist.get(sheet), columns.get(col))
                            );
                    }
                }
            }
        }
        return payloadstring;
    }

    protected String handleuserDefinedVariables(String payloadstring) {
        Collection<Object> valuelist = Control
            .getCurrentProject()
            .getProjectSettings()
            .getUserDefinedSettings()
            .values();
        for (Object prop : valuelist) {
            if (payloadstring.contains("{" + prop + "}")) {
                payloadstring = payloadstring.replace("{" + prop + "}", prop.toString());
            }
        }
        return payloadstring;
    }

    protected void OpenURLconnection() {
        try {
            httpRequestBuilder.put(key, HttpRequest.newBuilder());
            URI uri = URI.create(endPoints.get(key));
            httpRequestBuilder.put(key, httpRequestBuilder.get(key).uri(uri));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    protected void setheaders() {
        try {
            if (headers.containsKey(key)) {
                ArrayList<String> headerlist = headers.get(key);
                System.out.println(headerlist);
                if (headerlist.size() > 0) {
                    headerlist.forEach(
                        header -> {
                            httpRequestBuilder.put(
                                key,
                                httpRequestBuilder
                                    .get(key)
                                    .setHeader(
                                        header.substring(0, header.indexOf("=")),
                                        header.substring(header.indexOf("=") + 1, header.length())
                                    )
                            );
                        }
                    );
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    protected void httpAgentCheck() {
        try {
            if (getHttpAgentDetails() != null) {
                System.setProperty("http.agent", getHttpAgentDetails());
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    protected KeyManager[] loadKeyStore() {
        String keystorePath = Control
            .getCurrentProject()
            .getProjectSettings()
            .getDriverSettings()
            .getKeyStorePath();
        String keystorePassword = Control
            .getCurrentProject()
            .getProjectSettings()
            .getDriverSettings()
            .getKeyStorePassword();
        KeyStore keyStore;
        KeyManagerFactory kmf = null;
        try {
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keystorePassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            tmf.init(keyStore);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
        return kmf.getKeyManagers();
    }

    protected Boolean isSSLCertificateVerification() {
        return Control
            .getCurrentProject()
            .getProjectSettings()
            .getDriverSettings()
            .sslCertificateVerification();
    }

    protected Boolean isSelfSigned() {
        return Control.getCurrentProject().getProjectSettings().getDriverSettings().selfSigned();
    }

    /**
     * WebservicePluginApi implementation methods for direct map access
     */

    /**
     * Gets the context key for webservice operations.
     * @return the key used to index webservice maps
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * Gets direct access to the shared endpoints map.
     * @return the static endPoints map
     */
    @Override
    public Map<String, String> getEndPointsMap() {
        return endPoints;
    }

    /**
     * Gets direct access to the shared headers map.
     * @return the static headers map
     */
    @Override
    public Map<String, ArrayList<String>> getHeadersMap() {
        return headers;
    }

    /**
     * Gets direct access to the shared URL parameters map.
     * @return the static urlParams map
     */
    @Override
    public Map<String, ArrayList<String>> getUrlParamsMap() {
        return urlParams;
    }

    /**
     * Gets direct access to the shared response bodies map.
     * @return the static responsebodies map
     */
    @Override
    public Map<String, String> getResponseBodiesMap() {
        return responsebodies;
    }

    /**
     * Gets direct access to the shared response codes map.
     * @return the static responsecodes map
     */
    @Override
    public Map<String, String> getResponseCodesMap() {
        return responsecodes;
    }

    /**
     * Gets direct access to the shared response messages map.
     * @return the static responsemessages map
     */
    @Override
    public Map<String, String> getResponseMessagesMap() {
        return responsemessages;
    }

    /**
     * Gets a driver property value from the current project settings.
     * This respects the current API config context set by setEndPoint.
     * @param propertyKey the property key to retrieve
     * @return the property value, or null if not found
     */
    @Override
    public String getDriverProperty(String propertyKey) {
        try {
            String value = Control
                .getCurrentProject()
                .getProjectSettings()
                .getDriverSettings()
                .getAPIConfigProperty(propertyKey);
            return (value != null && !value.isEmpty()) ? value : null;
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
            return null;
        }
    }
}
