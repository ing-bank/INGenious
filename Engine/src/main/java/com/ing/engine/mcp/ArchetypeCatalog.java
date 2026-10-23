package com.ing.engine.mcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A registry of test-case <b>archetypes</b> &mdash; opinionated templates that
 * encode INGenious best practice for a given kind of test (browser flow, API
 * request, DB check, &hellip;). Each archetype is a small ordered list of
 * {@link Step}s whose {@code object}/{@code input} fields may contain
 * {@code ${token}} placeholders that {@code ingenious_gen_testcase} substitutes
 * from caller-supplied parameters.
 *
 * <p>All action names below were verified against the live
 * {@link ActionCatalog} so generated test cases reference real actions.
 *
 * <p>Shared by the MCP server ({@code ingenious_gen_*}) and available as the
 * {@code ingenious://catalog/archetypes} resource.
 */
public final class ArchetypeCatalog {

    private ArchetypeCatalog() {}

    /** One templated step. Tokens look like {@code ${url}}. */
    public static final class Step {
        public final String action;
        public final String object;
        public final String input;
        public final String description;

        public Step(String action, String object, String input, String description) {
            this.action = action;
            this.object = object;
            this.input = input;
            this.description = description;
        }
    }

    /** A named template. */
    public static final class Archetype {
        public final String name;
        public final String category;
        public final String description;
        public final List<String> parameters;
        public final List<Step> steps;

        Archetype(
            String name,
            String category,
            String description,
            List<String> parameters,
            List<Step> steps
        ) {
            this.name = name;
            this.category = category;
            this.description = description;
            this.parameters = parameters;
            this.steps = steps;
        }
    }

    private static final Map<String, Archetype> REGISTRY = new LinkedHashMap<>();

    private static void add(
        String name,
        String category,
        String description,
        List<String> params,
        Step... steps
    ) {
        REGISTRY.put(
            name.toLowerCase(Locale.ROOT),
            new Archetype(name, category, description, params, java.util.Arrays.asList(steps))
        );
    }

    private static List<String> params(String... p) {
        return java.util.Arrays.asList(p);
    }

    static {
        // ---- Browser ---------------------------------------------------
        add(
            "browser-login",
            "Browser",
            "Open a site, sign in, and verify a post-login element.",
            params(
                "url",
                "userField",
                "username",
                "passField",
                "password",
                "loginButton",
                "dashboard"
            ),
            new Step("Open", "", "@${url}", "Open the login page"),
            new Step("Fill", "${userField}", "@${username}", "Enter username"),
            new Step("Fill", "${passField}", "@${password}", "Enter password"),
            new Step("Click", "${loginButton}", "", "Submit the login form"),
            new Step("waitForElementToBeVisible", "${dashboard}", "", "Wait for the dashboard"),
            new Step("assertElementIsVisible", "${dashboard}", "", "Verify login succeeded"),
            new Step("ClosePage", "", "", "Close the page")
        );

        add(
            "browser-flow",
            "Browser",
            "Open a page, click an element, and assert visible text.",
            params("url", "element", "target", "expectedText"),
            new Step("Open", "", "@${url}", "Open the page"),
            new Step("waitForElementToBeVisible", "${element}", "", "Wait for the element"),
            new Step("Click", "${element}", "", "Interact with the element"),
            new Step("assertElementContainsText", "${target}", "@${expectedText}", "Verify result"),
            new Step("ClosePage", "", "", "Close the page")
        );

        add(
            "browser-search",
            "Browser",
            "Open a site, type a query, submit, and verify results appear.",
            params("url", "searchBox", "query", "results"),
            new Step("Open", "", "@${url}", "Open the site"),
            new Step("Fill", "${searchBox}", "@${query}", "Type the search query"),
            new Step("PressSequentially", "${searchBox}", "@Enter", "Submit the search"),
            new Step("waitForElementToBeVisible", "${results}", "", "Wait for results"),
            new Step("assertElementIsVisible", "${results}", "", "Verify results are shown"),
            new Step("ClosePage", "", "", "Close the page")
        );

        // ---- API -------------------------------------------------------
        add(
            "api-get",
            "API",
            "GET a URL and assert the HTTP status code.",
            params("url", "status"),
            new Step("setEndPoint", "Webservice", "@${url}", "Set the request endpoint"),
            new Step("getRestRequest", "Webservice", "", "Send a GET request"),
            new Step("assertResponseCode", "Webservice", "@${status}", "Verify the status code")
        );

        add(
            "api-post",
            "API",
            "POST a JSON body to a URL and assert the status code.",
            params("url", "body", "status"),
            new Step("setEndPoint", "Webservice", "@${url}", "Set the request endpoint"),
            new Step(
                "addHeader",
                "Webservice",
                "@Content-Type: application/json",
                "JSON content type"
            ),
            new Step("postRestRequest", "Webservice", "@${body}", "Send a POST request"),
            new Step("assertResponseCode", "Webservice", "@${status}", "Verify the status code")
        );

        add(
            "api-json-verify",
            "API",
            "GET a URL, assert 200, and verify a JSON element value.",
            params("url", "jsonPath", "expected"),
            new Step("setEndPoint", "Webservice", "@${url}", "Set the request endpoint"),
            new Step("getRestRequest", "Webservice", "", "Send a GET request"),
            new Step("assertResponseCode", "Webservice", "@200", "Verify HTTP 200"),
            new Step("assertJSONelementEquals", "Webservice", "@${jsonPath}", "Verify JSON value")
        );

        // ---- Hybrid ----------------------------------------------------
        add(
            "e2e-ui-then-api",
            "General",
            "Do a UI action, then confirm the effect through an API call.",
            params("url", "element", "apiUrl", "jsonPath", "expected"),
            new Step("Open", "", "@${url}", "Open the UI"),
            new Step("Click", "${element}", "", "Perform the UI action"),
            new Step("ClosePage", "", "", "Close the UI"),
            new Step("setEndPoint", "Webservice", "@${apiUrl}", "Set the verification endpoint"),
            new Step("getRestRequest", "Webservice", "", "Fetch the record"),
            new Step("assertResponseCode", "Webservice", "@200", "Verify the API responded"),
            new Step("assertJSONelementEquals", "Webservice", "@${expected}", "Verify the effect")
        );
    }

    public static Collection<Archetype> all() {
        return REGISTRY.values();
    }

    public static Archetype find(String name) {
        return name == null ? null : REGISTRY.get(name.toLowerCase(Locale.ROOT));
    }

    /** Substitute {@code ${token}} placeholders using {@code values}; unknown tokens are left intact. */
    public static String substitute(String template, Map<String, String> values) {
        if (template == null || template.indexOf("${") < 0) return template;
        String out = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("${" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    /** Tokens still unresolved in a string (for reporting to the caller). */
    public static List<String> unresolvedTokens(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}").matcher(s);
        while (m.find()) out.add(m.group(1));
        return out;
    }
}
