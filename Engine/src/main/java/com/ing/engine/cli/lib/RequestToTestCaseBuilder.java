package com.ing.engine.cli.lib;

import com.ing.datalib.api.APIAssertion;
import com.ing.datalib.api.APIRequest;
import com.ing.datalib.api.AuthConfig;
import com.ing.datalib.api.KeyValuePair;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;

import java.util.Base64;
import java.util.List;

/**
 * Engine-side, headless converter from {@link APIRequest} → INGenious
 * Webservice test case steps.
 *
 * <p>This is a slimmed-down version of the IDE's {@code APITester
 * .buildStepsForRequest}: it covers the things that a CLI / MCP user
 * actually exercises when importing a curl command or a
 * Postman / Bruno collection – endpoint, headers, auth, body, basic
 * assertions – without depending on the IDE's UI tree, OR refresher
 * or Structured Data object creation.
 */
public final class RequestToTestCaseBuilder {

    private RequestToTestCaseBuilder() {}

    /**
     * Create a new test case named {@code tcName} under {@code scenario}
     * and populate it with steps derived from {@code request}. Returns
     * {@code null} if the test case already exists.
     */
    public static TestCase build(APIRequest request, Scenario scenario, String tcName) {
        if (request == null || scenario == null || tcName == null) return null;
        TestCase tc = scenario.addTestCase(tcName);
        if (tc == null) return null;
        request.setName(tcName);
        buildSteps(tc, request);
        tc.save();
        return tc;
    }

    /**
     * Append API steps for {@code request} to an existing {@code tc} (no
     * save). Useful when stacking multiple requests onto the same test case.
     */
    public static void appendSteps(TestCase tc, APIRequest request) {
        if (tc == null || request == null) return;
        buildSteps(tc, request);
    }

    private static void buildSteps(TestCase tc, APIRequest req) {
        // 1. setEndPoint
        TestStep ep = tc.addNewStep();
        ep.setObject("Webservice");
        ep.setDescription("Set API Endpoint");
        ep.setAction("setEndPoint");
        ep.setInput("@" + resolveUrl(req));

        // 2. headers
        if (req.getHeaders() != null) {
            for (KeyValuePair h : req.getHeaders()) {
                if (h == null || !h.isEnabled()) continue;
                if (h.getKey() == null || h.getKey().isEmpty()) continue;
                TestStep s = tc.addNewStep();
                s.setObject("Webservice");
                s.setDescription("Add Header: " + h.getKey());
                s.setAction("addHeader");
                s.setInput("@" + h.getKey() + "=" + safe(h.getValue()));
            }
        }

        // 3. auth
        addAuthSteps(tc, req.getAuth());

        // 4. HTTP method
        TestStep call = tc.addNewStep();
        call.setObject("Webservice");
        call.setDescription("Execute " + req.getMethod() + " Request");
        String body = (req.getBody() != null && req.getBody().getRawContent() != null)
                ? req.getBody().getRawContent() : "";
        switch (req.getMethod() == null ? APIRequest.HttpMethod.GET : req.getMethod()) {
            case GET:
                call.setAction("getRestRequest");
                break;
            case POST:
                call.setAction("postRestRequest");
                if (!body.isEmpty()) call.setInput(body);
                break;
            case PUT:
                call.setAction("putRestRequest");
                if (!body.isEmpty()) call.setInput(body);
                break;
            case PATCH:
                call.setAction("patchRestRequest");
                if (!body.isEmpty()) call.setInput(body);
                break;
            case DELETE:
                if (!body.isEmpty()) {
                    call.setAction("deleteWithPayload");
                    call.setInput(body);
                } else {
                    call.setAction("deleteRestRequest");
                }
                break;
            default:
                call.setAction("getRestRequest");
        }

        // 5. assertions (basic types only - no OR object creation)
        addAssertionSteps(tc, req);
    }

    private static String resolveUrl(APIRequest req) {
        String url = req.getUrl() == null ? "" : req.getUrl();
        if (req.getQueryParams() != null && !req.getQueryParams().isEmpty()) {
            StringBuilder qs = new StringBuilder();
            for (KeyValuePair p : req.getQueryParams()) {
                if (p == null || !p.isEnabled()) continue;
                if (qs.length() > 0) qs.append('&');
                qs.append(safe(p.getKey())).append('=').append(safe(p.getValue()));
            }
            if (qs.length() > 0) {
                url += (url.contains("?") ? "&" : "?") + qs;
            }
        }
        return url;
    }

    private static void addAuthSteps(TestCase tc, AuthConfig auth) {
        if (auth == null || auth.getAuthType() == AuthConfig.AuthType.NONE) return;
        TestStep s = tc.addNewStep();
        s.setObject("Webservice");
        s.setAction("addHeader");
        switch (auth.getAuthType()) {
            case BASIC: {
                s.setDescription("Add Basic Auth Header");
                String pair = safe(auth.getBasicUsername()) + ":" + safe(auth.getBasicPassword());
                String enc = Base64.getEncoder().encodeToString(pair.getBytes());
                s.setInput("@Authorization=Basic " + enc);
                break;
            }
            case BEARER: {
                s.setDescription("Add Bearer Token Header");
                String prefix = auth.getBearerPrefix() != null ? auth.getBearerPrefix() : "Bearer";
                s.setInput("@Authorization=" + prefix + " " + safe(auth.getBearerToken()));
                break;
            }
            case API_KEY: {
                String keyName = auth.getApiKeyName() != null ? auth.getApiKeyName() : "X-API-Key";
                s.setDescription("Add API Key Header: " + keyName);
                s.setInput("@" + keyName + "=" + safe(auth.getApiKeyValue()));
                break;
            }
            default:
                tc.getTestSteps().remove(s);
        }
    }

    private static void addAssertionSteps(TestCase tc, APIRequest req) {
        List<APIAssertion> assertions = req.getAssertions();
        if (assertions == null || assertions.isEmpty()) return;
        for (APIAssertion a : assertions) {
            if (a == null || !a.isEnabled() || a.getType() == null) continue;
            switch (a.getType()) {
                case STATUS_CODE: {
                    TestStep s = tc.addNewStep();
                    s.setObject("Webservice");
                    s.setDescription("Assert Response Code");
                    s.setAction("assertResponseCode");
                    s.setInput(prefixAt(a.getExpectedValue()));
                    break;
                }
                case BODY_CONTAINS: {
                    TestStep s = tc.addNewStep();
                    s.setObject("Webservice");
                    s.setDescription("Assert Response Body Contains");
                    s.setAction("assertResponsebodycontains");
                    s.setInput(prefixAt(a.getExpectedValue()));
                    break;
                }
                case HEADER: {
                    TestStep s = tc.addNewStep();
                    s.setObject("Webservice");
                    s.setDescription("Assert Header: " + a.getTarget());
                    s.setAction(a.getOperator() == APIAssertion.Operator.CONTAINS
                            ? "assertHeaderValueContains" : "assertHeaderValueEquals");
                    s.setCondition(safe(a.getTarget()));
                    s.setInput(prefixAt(a.getExpectedValue()));
                    break;
                }
                case JSON_PATH:
                case XPATH: {
                    // Engine-side fall-back: emit a generic webservice step
                    // (avoids the IDE-only Structured Data object machinery).
                    TestStep s = tc.addNewStep();
                    s.setObject("Webservice");
                    boolean xp = a.getType() == APIAssertion.AssertionType.XPATH;
                    s.setDescription("Assert " + (xp ? "XPath" : "JSON") + ": " + a.getTarget());
                    s.setAction(a.getOperator() == APIAssertion.Operator.CONTAINS
                            ? "assertJSONelementContains" : "assertJSONelementEquals");
                    s.setCondition(safe(a.getTarget()));
                    s.setInput(prefixAt(a.getExpectedValue()));
                    break;
                }
                default:
                    // unsupported assertion - silently skip
            }
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String prefixAt(String v) {
        if (v == null || v.isEmpty()) return "";
        return v.startsWith("@") ? v : "@" + v;
    }
}
