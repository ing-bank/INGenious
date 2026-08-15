package com.ing.engine.perf;

import java.util.ArrayList;
import java.util.List;

/**
 * One HTTP request to emit into a generated k6 script. Produced from either
 * an API test case ({@link K6HttpScriptGenerator#fromTestCase}) or a HAR
 * recording ({@link HarReader}).
 */
public final class HttpRequestSpec {
    /** Group label, e.g. "GET /users/1". */
    public String name;
    /** Lower-case HTTP method: get, post, put, patch, delete. */
    public String method = "get";
    public String url = "";
    /** Request payload; null/empty when the method carries no body. */
    public String body;
    /** Header pairs [name, value] in declaration order. */
    public final List<String[]> headers = new ArrayList<>();
    /** Expected response status to check(); null = no status check. */
    public Integer checkStatus;
    /** Free-form notes emitted as comments inside the group (e.g. TODOs). */
    public final List<String> comments = new ArrayList<>();

    // ---- correlation / rules support (Phase 6) ----------------------

    /**
     * Observed response body from the recording (HAR only); used by the
     * auto-correlation heuristic, never emitted into scripts.
     */
    public String recordedResponseBody;

    /**
     * Credential headers removed by the scrubber, kept ONLY for correlation
     * analysis (e.g. a token that reappears in Authorization). Never emitted.
     */
    public final List<String[]> scrubbedHeaders = new ArrayList<>();

    /**
     * Correlation captures to emit AFTER this request:
     * {@code const <var> = <jsExpression using res>;}
     * (jsExpression references the group's {@code res} variable).
     */
    public final List<String[]> captures = new ArrayList<>();

    /** Extra body-contains checks added by verification rules. */
    public final List<String> bodyContainsChecks = new ArrayList<>();

    public boolean carriesBody() {
        return (
            (
                "post".equals(method) ||
                "put".equals(method) ||
                "patch".equals(method) ||
                "delete".equals(method)
            ) &&
            body != null &&
            !body.isEmpty()
        );
    }
}
