package com.ing.engine.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Phase 6 conformance: rule model YAML round-trip, rule application
 * (correlation / parameterization / verification / header filters),
 * auto-correlation proposals and template-literal emission.
 */
public class RuleEngineTest {

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private static List<HttpRequestSpec> loginFlow() {
        HttpRequestSpec login = new HttpRequestSpec();
        login.method = "post";
        login.url = "https://api.example.com/login";
        login.name = "POST /login";
        login.body = "{\"user\":\"john\",\"pass\":\"secret1\"}";
        login.checkStatus = 200;
        login.recordedResponseBody =
            "{\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiJ9.abc123def456\",\"user\":\"john\"}}";

        HttpRequestSpec order = new HttpRequestSpec();
        order.method = "post";
        order.url = "https://api.example.com/orders";
        order.name = "POST /orders";
        order.body = "{\"item\":42}";
        order.checkStatus = 201;
        // the recorded token was scrubbed from Authorization at HAR import
        order.scrubbedHeaders.add(
            new String[] { "Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.abc123def456" }
        );

        HttpRequestSpec byToken = new HttpRequestSpec();
        byToken.method = "get";
        byToken.url = "https://api.example.com/session?t=eyJhbGciOiJIUzI1NiJ9.abc123def456";
        byToken.name = "GET /session";

        List<HttpRequestSpec> flow = new ArrayList<>();
        flow.add(login);
        flow.add(order);
        flow.add(byToken);
        return flow;
    }

    // ------------------------------------------------------------------
    // proposals
    // ------------------------------------------------------------------

    @Test
    public void proposesCorrelationForTokenReappearingLater() {
        List<PerfRule> proposals = RuleEngine.proposeCorrelations(loginFlow());
        assertThat(proposals).hasSize(1);
        PerfRule rule = proposals.get(0);
        assertThat(rule.type).isEqualTo("correlation");
        assertThat(rule.name).isEqualTo("token");
        assertThat(rule.extractSelector).isEqualTo("data.token");
        assertThat(rule.extractSource).isEqualTo("/login");
        assertThat(rule.value).isEqualTo("eyJhbGciOiJIUzI1NiJ9.abc123def456");
    }

    @Test
    public void shortOrNonRecurringValuesAreNotProposed() {
        List<HttpRequestSpec> flow = loginFlow();
        // "john" is < 8 chars; nothing else recurs once the token rule is out
        flow.get(1).scrubbedHeaders.clear();
        flow.remove(2);
        assertThat(RuleEngine.proposeCorrelations(flow)).isEmpty();
    }

    // ------------------------------------------------------------------
    // application
    // ------------------------------------------------------------------

    @Test
    public void correlationRewritesUrlAndReinjectsScrubbedHeader() {
        List<HttpRequestSpec> flow = loginFlow();
        List<PerfRule> rules = RuleEngine.proposeCorrelations(flow);
        RuleEngine.Result result = RuleEngine.apply(flow, rules);

        assertThat(result.applied).isEqualTo(1);
        assertThat(result.captureVars).containsExactly("corr_token");
        // capture on the source request
        assertThat(flow.get(0).captures).hasSize(1);
        assertThat(flow.get(0).captures.get(0)[0]).isEqualTo("corr_token");
        assertThat(flow.get(0).captures.get(0)[1]).isEqualTo("res.json('data.token')");
        // scrubbed Authorization header is re-injected with the placeholder
        assertThat(flow.get(1).headers).hasSize(1);
        assertThat(flow.get(1).headers.get(0)[0]).isEqualTo("Authorization");
        assertThat(flow.get(1).headers.get(0)[1]).isEqualTo("Bearer ${corr_token}");
        assertThat(flow.get(1).scrubbedHeaders).isEmpty();
        // URL occurrence rewritten
        assertThat(flow.get(2).url).isEqualTo("https://api.example.com/session?t=${corr_token}");
    }

    @Test
    public void endToEndScriptEmissionWithCorrelation() {
        List<HttpRequestSpec> flow = loginFlow();
        List<PerfRule> rules = RuleEngine.proposeCorrelations(flow);
        RuleEngine.Result applied = RuleEngine.apply(flow, rules);
        String script = K6HttpScriptGenerator.generate(
            "rec.har",
            null,
            PerfProfile.builtIn("smoke"),
            flow,
            new ArrayList<String>(),
            applied
        );
        assertThat(script).contains("let corr_token;");
        assertThat(script).contains("corr_token = res.json('data.token');");
        assertThat(script).contains("'Authorization': `Bearer ${corr_token}`,");
        assertThat(script).contains("http.get(`https://api.example.com/session?t=${corr_token}`);");
        // the plain-text token never leaks into the script
        assertThat(script).doesNotContain("abc123def456");
    }

    @Test
    public void parameterizationEnvAndVerificationAndHeaderFilter() {
        List<HttpRequestSpec> flow = loginFlow();
        List<PerfRule> rules = new ArrayList<>();

        PerfRule param = new PerfRule();
        param.type = "parameterization";
        param.matchIn = "body";
        param.matchLiteral = "john";
        param.valueSource = "env";
        param.valueName = "USER_NAME";
        rules.add(param);

        PerfRule verify = new PerfRule();
        verify.type = "verification";
        verify.matchUrl = "/orders";
        verify.checkStatus = 201;
        verify.checkBodyContains = "orderId";
        rules.add(verify);

        PerfRule filter = new PerfRule();
        filter.type = "headerFilter";
        filter.header = "X-Trace";
        filter.action = "drop";
        rules.add(filter);
        flow.get(1).headers.add(new String[] { "X-Trace", "abc" });

        RuleEngine.Result result = RuleEngine.apply(flow, rules);
        assertThat(result.applied).isEqualTo(3);
        assertThat(flow.get(0).body).contains("${__ENV.USER_NAME}");
        assertThat(flow.get(1).bodyContainsChecks).containsExactly("orderId");
        assertThat(flow.get(1).headers).isEmpty();

        String script = K6HttpScriptGenerator.generate(
            "x",
            null,
            PerfProfile.builtIn("smoke"),
            flow,
            new ArrayList<String>(),
            result
        );
        assertThat(script).contains("${__ENV.USER_NAME}");
        assertThat(script).contains("body contains orderId");
    }

    @Test
    public void dataFileParameterizationEmitsSharedArray() {
        HttpRequestSpec request = new HttpRequestSpec();
        request.method = "post";
        request.url = "https://api.example.com/login";
        request.name = "POST /login";
        request.body = "{\"user\":\"recordedUser\"}";
        List<HttpRequestSpec> flow = new ArrayList<>();
        flow.add(request);

        PerfRule rule = new PerfRule();
        rule.type = "parameterization";
        rule.matchIn = "body";
        rule.matchLiteral = "recordedUser";
        rule.valueSource = "dataFile";
        rule.valueFile = "users.json";
        rule.valueName = "username";
        List<PerfRule> rules = new ArrayList<>();
        rules.add(rule);

        RuleEngine.Result result = RuleEngine.apply(flow, rules);
        assertThat(result.dataFiles).containsEntry("data_users", "users.json");
        String script = K6HttpScriptGenerator.generate(
            "x",
            null,
            PerfProfile.builtIn("smoke"),
            flow,
            new ArrayList<String>(),
            result
        );
        assertThat(script).contains("import { SharedArray } from 'k6/data';");
        assertThat(script)
            .contains("const data_users = new SharedArray('data_users', function () {");
        assertThat(script).contains("open('../data/users.json')");
        assertThat(script).contains("${data_users[__ITER % data_users.length].username}");
    }

    // ------------------------------------------------------------------
    // YAML round-trip + template escaping
    // ------------------------------------------------------------------

    @Test
    public void rulesYamlRoundTrip() throws Exception {
        File dir = Files.createTempDirectory("rules-test").toFile();
        try {
            List<PerfRule> rules = RuleEngine.proposeCorrelations(loginFlow());
            PerfRule filter = new PerfRule();
            filter.type = "headerFilter";
            filter.header = "X-Trace";
            filter.action = "redact";
            rules.add(filter);
            File file = new File(dir, "s.rules.yaml");
            PerfRule.save(rules, file);
            List<PerfRule> loaded = PerfRule.load(file);
            assertThat(loaded).hasSize(2);
            assertThat(loaded.get(0).type).isEqualTo("correlation");
            assertThat(loaded.get(0).value).isEqualTo(rules.get(0).value);
            assertThat(loaded.get(0).extractSelector).isEqualTo("data.token");
            assertThat(loaded.get(1).type).isEqualTo("headerFilter");
            assertThat(loaded.get(1).action).isEqualTo("redact");
        } finally {
            for (File f : dir.listFiles()) {
                f.delete();
            }
            dir.delete();
        }
    }

    @Test
    public void templateLiteralsEscapeHostileContentButKeepPlaceholders() {
        assertThat(K6HttpScriptGenerator.jsValue("plain")).isEqualTo("'plain'");
        assertThat(K6HttpScriptGenerator.jsValue("a ${corr_x} b")).isEqualTo("`a ${corr_x} b`");
        // backticks and foreign interpolations are neutralized
        assertThat(K6HttpScriptGenerator.jsValue("`${evil}` ${corr_x}"))
            .isEqualTo("`\\`\\${evil}\\` ${corr_x}`");
        assertThat(K6HttpScriptGenerator.jsValue("${__ENV.USER} x")).isEqualTo("`${__ENV.USER} x`");
    }
}
