package com.ing.ide.main.mainui.components.aichat.skills;

import com.ing.ide.main.mainui.components.aichat.model.PromptTemplate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of the built-in prompt library shown as clickable chips in the AI
 * assistant sidebar. Mirrors the sections of the MCP Getting Started guide so
 * users can drive the full tool surface without memorising prompts.
 *
 * <p>Templates use {@code ${currentProject}}, {@code ${currentScenario}}, and
 * {@code ${currentTestCase}} tokens which are substituted with the live IDE
 * selection when a chip is clicked.</p>
 */
public final class PromptLibrary {
    private static final List<PromptTemplate> PROMPTS = new ArrayList<>();

    private PromptLibrary() {}

    static {
        // ── Explore ──────────────────────────────────────────────────────
        add(
            "explore-1a",
            "Explore",
            "Orientation",
            "What INGenious projects are available? For each one, tell me how " +
            "many scenarios and test cases it contains."
        );
        add(
            "explore-1b",
            "Explore",
            "Drill into scenario",
            "Show me everything inside the ${currentScenario} scenario of " +
            "${currentProject} — list all test cases with their step counts, " +
            "then show me the full steps for ${currentTestCase}.",
            "currentProject",
            "currentScenario",
            "currentTestCase"
        );
        add(
            "explore-1c",
            "Explore",
            "Action vocabulary",
            "What categories of actions are available? List all Browser actions " +
            "and show me the full parameter list for the \"Click\" action."
        );
        add(
            "explore-1d",
            "Explore",
            "Find action by intent",
            "I need to verify that a JSON response field equals a specific value. " +
            "Which action should I use and what arguments does it take?"
        );

        // ── Author ───────────────────────────────────────────────────────
        add(
            "author-2a",
            "Author",
            "API test from scratch",
            "In ${currentProject}, create a new scenario called \"HealthChecks\". " +
            "Inside it, create a test case called \"PingAPI\" that:\n" +
            "  1. Sets the endpoint to https://jsonplaceholder.typicode.com/users/1\n" +
            "  2. Sends a GET request\n" +
            "  3. Verifies the response code is 200\n" +
            "  4. Verifies the JSON field \"name\" equals \"Leanne Graham\"\n\n" +
            "Look up the correct action names before creating the steps.",
            "currentProject"
        );
        add(
            "author-2b",
            "Author",
            "Browser test from scratch",
            "Create a test case called \"SearchGoogle\" in the scenario " +
            "\"BrowserSamples\" (create the scenario if it doesn't exist). " +
            "The test should:\n" +
            "  1. Open a browser\n" +
            "  2. Navigate to https://www.google.com\n" +
            "  3. Type \"INGenious test automation\" into the search box\n" +
            "  4. Press Enter\n" +
            "  5. Wait for results to appear\n" +
            "  6. Verify that at least one result is visible\n" +
            "  7. Close the page\n\n" +
            "Use the correct INGenious browser action names."
        );
        add(
            "author-2c",
            "Author",
            "Convert manual steps",
            "I have these manual test steps for a login flow — convert them to a " +
            "proper INGenious test case called \"LoginHappy\" in scenario " +
            "\"Auth\":\n\n" +
            "1. Open Chrome\n" +
            "2. Go to https://demo.example.com/login\n" +
            "3. Enter \"alice@example.com\" in the username field\n" +
            "4. Enter \"hunter2\" in the password field\n" +
            "5. Click the Login button\n" +
            "6. Wait for the dashboard to load\n" +
            "7. Verify the welcome message says \"Welcome, Alice\"\n" +
            "8. Close the browser"
        );
        add(
            "author-2d",
            "Author",
            "Edit steps",
            "In the ${currentTestCase} test case, insert a step after step 3 that " +
            "waits for the target element to be visible before the next action. " +
            "Then move the \"Close page\" step to be second-to-last.",
            "currentTestCase"
        );

        // ── Object Repo ────────────────────────────────────────────────────
        add(
            "or-3a",
            "Object Repo",
            "Explore repository",
            "List all Object Repository pages in ${currentProject}. Then show me " +
            "every locator defined on the first page. Is there anything matching " +
            "\"search\" or \"query\"?",
            "currentProject"
        );
        add(
            "or-3b",
            "Object Repo",
            "Add locators",
            "Add these locators to the ${currentProject} Object Repository under a " +
            "page called \"GoogleSearchPage\":\n" +
            "  - \"search.input\" — CSS selector: input[name='q']\n" +
            "  - \"search.submit\" — XPath: //input[@name='btnK']\n" +
            "  - \"results.container\" — CSS selector: #search",
            "currentProject"
        );
        add(
            "or-3c",
            "Object Repo",
            "Scrape a live page",
            "Open https://demo.testfire.net/login.jsp in a headless browser and " +
            "import all the locators you find into a page called " +
            "\"AltitixLoginPage\" in ${currentProject}.",
            "currentProject"
        );
        add(
            "or-3d",
            "Object Repo",
            "Update & clean up",
            "In ${currentProject}'s Object Repository, find the locator " +
            "\"search.submit\" on GoogleSearchPage and update it to use CSS " +
            "selector: button[type=submit]. Then delete the \"results.container\" " +
            "locator — we won't use it.",
            "currentProject"
        );

        // ── Data ───────────────────────────────────────────────────────────
        add(
            "data-4a",
            "Data",
            "Inspect existing data",
            "What environments exist in ${currentProject}? Show me the contents of " +
            "any data sheets that are already set up.",
            "currentProject"
        );
        add(
            "data-4b",
            "Data",
            "Create data sheet",
            "Create a new data sheet called \"LoginUsers\" in ${currentProject} " +
            "with columns: username, password, expectedRole. Add three rows for " +
            "the \"dev\" environment:\n" +
            "  Row 1: alice@example.com / pass1 / admin\n" +
            "  Row 2: bob@example.com / pass2 / editor\n" +
            "  Row 3: carol@example.com / pass3 / viewer",
            "currentProject"
        );
        add(
            "data-4c",
            "Data",
            "Generate synthetic data",
            "Generate 20 rows of synthetic user data for a sheet called " +
            "\"SyntheticUsers\" in ${currentProject}. I need these columns: " +
            "firstname, lastname, email, phone, city, and a random integer " +
            "\"age\" between 18 and 65. Use seed 42 so results are reproducible.",
            "currentProject"
        );
        add(
            "data-4d",
            "Data",
            "Import from CSV",
            "I have a CSV file at /tmp/test_products.csv with columns: productId, " +
            "name, price, category. Import it into ${currentProject} as a data " +
            "sheet called \"Products\" in the \"staging\" environment (create " +
            "that environment first if needed), then show me the first 10 rows " +
            "to confirm.",
            "currentProject"
        );

        // ── Generate ─────────────────────────────────────────────────────
        add(
            "gen-5a",
            "Generate",
            "Explore archetypes",
            "List all available test archetypes. For the \"browser-login\" " +
            "archetype, show me exactly what parameters I need to supply and " +
            "what steps it will generate."
        );
        add(
            "gen-5b",
            "Generate",
            "Generate from archetype",
            "Use the \"api-get\" archetype to create a test case called " +
            "\"FetchPost\" in scenario \"GeneratedSuite\" in ${currentProject}. " +
            "The target URL is https://jsonplaceholder.typicode.com/posts/1 and " +
            "the expected status is 200. After generating, validate the test case.",
            "currentProject"
        );
        add(
            "gen-5c",
            "Generate",
            "Preview with dry run",
            "Preview what the \"browser-login\" archetype would generate for a " +
            "test case called \"LoginPreview\" without actually creating any " +
            "files. Show me the parameters needed and the steps that would be " +
            "created."
        );
        add(
            "gen-5d",
            "Generate",
            "Import a curl command",
            "Convert this curl command into an INGenious test case called " +
            "\"CurlImport\" in scenario \"Imports\":\n\n" +
            "  curl -X POST https://api.example.com/auth \\\n" +
            "    -H \"Content-Type: application/json\" \\\n" +
            "    -d '{\"username\":\"alice\",\"password\":\"secret\"}'"
        );
        add(
            "gen-5e",
            "Generate",
            "Import OpenAPI spec",
            "I have an OpenAPI spec at /tmp/petstore.yaml. Generate one test case " +
            "per API operation in a new scenario called \"PetstoreAPI\" in " +
            "${currentProject}. Set the base URL to " +
            "https://petstore3.swagger.io/api/v3.",
            "currentProject"
        );
        add(
            "gen-5f",
            "Generate",
            "Import Postman/Bruno",
            "I exported my Postman collection to " +
            "/tmp/myapi.postman_collection.json. Import it into ${currentProject} " +
            "as scenario \"PostmanImport\", creating one test case per request.",
            "currentProject"
        );

        // ── Run ────────────────────────────────────────────────────────────
        add(
            "run-6a",
            "Run",
            "Dry run first",
            "I want to run the ${currentTestCase} test case in scenario " +
            "${currentScenario} of ${currentProject}, but first do a dry run to " +
            "check it's configured correctly and would actually execute. Then, " +
            "if everything looks good, run it for real.",
            "currentProject",
            "currentScenario",
            "currentTestCase"
        );
        add(
            "run-6b",
            "Run",
            "Async with status",
            "Start running all test cases in the ${currentScenario} scenario " +
            "asynchronously using chromium. Give me a run ID so I can check the " +
            "status. Then check the status every few seconds until it finishes.",
            "currentScenario"
        );
        add(
            "run-6c",
            "Run",
            "Cancel a run",
            "I accidentally kicked off a run with ID " +
            "\"20240704-150000-abc123\". Cancel it and show me how many steps " +
            "had completed before cancellation."
        );

        // ── Reports ──────────────────────────────────────────────────────
        add(
            "report-7a",
            "Reports",
            "Latest result",
            "What was the result of the last run for the ${currentTestCase} test " +
            "case in ${currentProject}? How long did it take and did anything " +
            "fail?",
            "currentProject",
            "currentTestCase"
        );
        add(
            "report-7b",
            "Reports",
            "Failure deep-dive",
            "The latest run of ${currentScenario}/${currentTestCase} had failures. " +
            "Show me each failed step with its error message and, if available, a " +
            "screenshot path. Suggest what might be wrong and how to fix it.",
            "currentScenario",
            "currentTestCase"
        );
        add(
            "report-7c",
            "Reports",
            "Compare two runs",
            "Compare the run from this morning (ID: 20240704-090000-old) with the " +
            "run from this afternoon (ID: 20240704-150000-new) for " +
            "${currentProject}. What test cases regressed? What got fixed? What " +
            "stayed the same?",
            "currentProject"
        );
        add(
            "report-7d",
            "Reports",
            "Export for CI",
            "Export the latest run report for ${currentProject} in JUnit XML " +
            "format so I can feed it into my Jenkins pipeline. Also export a CSV " +
            "summary to /tmp/results.csv.",
            "currentProject"
        );
        add(
            "report-7e",
            "Reports",
            "Trend analysis",
            "Show me the run history for ${currentScenario}/${currentTestCase} over " +
            "the last 10 runs. Is the pass rate improving or degrading? What's " +
            "the average duration?",
            "currentScenario",
            "currentTestCase"
        );

        // ── Test Sets ──────────────────────────────────────────────────────
        add(
            "testset-8a",
            "Test Sets",
            "Build regression suite",
            "Create a test set called \"DailyRegression\" under release \"v2.0\" " +
            "in ${currentProject}. Add every test case from the " +
            "${currentScenario} scenario to it, all running with chromium. Then " +
            "show me the full execution plan.",
            "currentProject",
            "currentScenario"
        );
        add(
            "testset-8b",
            "Test Sets",
            "Cross-browser plan",
            "I want to run ${currentScenario}/${currentTestCase} in three browsers. " +
            "Add it to a test set called \"CrossBrowser\" in release \"v2.0\" " +
            "three times — once each for Chrome, Firefox, and Edge. Use separate " +
            "iterations.",
            "currentScenario",
            "currentTestCase"
        );

        // ── Browser ────────────────────────────────────────────────────────
        add(
            "browser-9a",
            "Browser",
            "Record a live session",
            "Start a Playwright browser session in chromium. Navigate to " +
            "https://demo.testfire.net, click on \"Sign In\", fill in username " +
            "\"admin\" and password \"admin\", click the login button, then take " +
            "a snapshot so I can see what the page looks like. Save the recorded " +
            "steps as test case \"LiveLogin\" in scenario \"Recorded\"."
        );
        add(
            "browser-9b",
            "Browser",
            "Inspect an element",
            "I have a browser session open. Inspect the element at CSS selector " +
            "\"#account-summary\" and tell me the best locator strategy to use — " +
            "ID, CSS, or XPath? Add the best locator to the Object Repository " +
            "page \"DemoPage\" as \"account.summary\"."
        );

        // ── Preview ──────────────────────────────────────────────────────
        add(
            "preview-10a",
            "Preview",
            "Preview before commit",
            "Before creating anything, show me exactly what steps the " +
            "\"e2e-ui-then-api\" archetype would generate for a test called " +
            "\"E2EPreview\" in ${currentProject}. I want to see the full step " +
            "list without writing any files.",
            "currentProject"
        );
        add(
            "preview-10b",
            "Preview",
            "Safe idempotent create",
            "Create test case ${currentTestCase} in scenario ${currentScenario} of " +
            "${currentProject}. I know it might already exist — if it does, just " +
            "skip silently and tell me it's already there. Don't overwrite it.",
            "currentProject",
            "currentScenario",
            "currentTestCase"
        );
        add(
            "preview-10c",
            "Preview",
            "Recover from a typo",
            "Show me the test case \"GetAllUser\" in scenario \"APIBaiscs\" of " +
            "${currentProject}.",
            "currentProject"
        );
        add(
            "preview-10d",
            "Preview",
            "Health check",
            "Run a full health check on ${currentProject} — check that the JDK is " +
            "configured correctly, the browser drivers are available, Playwright " +
            "CLI is installed, and the project structure is valid. Fix anything " +
            "that can be auto-fixed.",
            "currentProject"
        );

        // ── Config ─────────────────────────────────────────────────────────
        add(
            "config-11a",
            "Config",
            "Read project config",
            "Show me the full configuration for ${currentProject} — what browser " +
            "is configured by default, what environments are set up, and what " +
            "driver paths are used?",
            "currentProject"
        );
        add(
            "config-11b",
            "Config",
            "Change a setting",
            "In ${currentProject}, change the default browser to Firefox and set " +
            "the implicit wait timeout to 10 seconds. Show me the config before " +
            "and after the change.",
            "currentProject"
        );

        // ── Gauntlet ─────────────────────────────────────────────────────
        add(
            "gauntlet-12",
            "Gauntlet",
            "Full end-to-end suite",
            "Let's build a complete, runnable test suite for the JSONPlaceholder " +
            "API from scratch in ${currentProject}. Here's what I need:\n\n" +
            "1. Create a new scenario called \"JSONPlaceholder\"\n\n" +
            "2. Look up the right API action names for: setting an endpoint, " +
            "sending GET/POST requests, checking the response code, and " +
            "extracting a JSON field.\n\n" +
            "3. Use the \"api-get\" archetype to generate a test case " +
            "\"GetPost1\" that fetches " +
            "https://jsonplaceholder.typicode.com/posts/1 and expects 200. " +
            "Preview it with dryRun first, then create it.\n\n" +
            "4. Create a test case \"CreatePost\" from scratch that:\n" +
            "   - POSTs to https://jsonplaceholder.typicode.com/posts\n" +
            "   - with body: {\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}\n" +
            "   - verifies the response code is 201\n" +
            "   - verifies the JSON field \"id\" is not empty\n\n" +
            "5. Create a data sheet \"PostIds\" with columns: postId, " +
            "expectedTitle. Generate 5 synthetic rows for the dev environment.\n\n" +
            "6. Build a test set \"Regression\" in release \"v1.0\" containing " +
            "both test cases, using chromium.\n\n" +
            "7. Run the test set with a dry run first, then actually execute it.\n\n" +
            "8. Show me the results — did both tests pass? If anything failed, " +
            "explain what went wrong and suggest a fix.\n\n" +
            "9. Export the report as JUnit XML to " +
            "/tmp/jsonplaceholder-results.xml.",
            "currentProject"
        );
    }

    private static void add(
        String id,
        String section,
        String label,
        String template,
        String... tokens
    ) {
        PROMPTS.add(PromptTemplate.of(id, section, label, template, tokens));
    }

    /** All prompts in registration order. */
    public static List<PromptTemplate> all() {
        return new ArrayList<>(PROMPTS);
    }

    /** Prompts grouped by section, preserving section and prompt order. */
    public static Map<String, List<PromptTemplate>> bySection() {
        Map<String, List<PromptTemplate>> map = new LinkedHashMap<>();
        for (PromptTemplate p : PROMPTS) {
            map.computeIfAbsent(p.getSection(), k -> new ArrayList<>()).add(p);
        }
        return map;
    }
}
