# INGenious MCP — Getting Started Guide

> **What this is:** Once your AI agent (Claude, GitHub Copilot, Cursor, etc.) is connected to the MCP server you talk to it in plain English. This guide gives you **copy-paste prompts** organised from beginner to advanced so you can explore the full 75-tool surface in a single session.
>
> Need setup help first? See [MCP User Manual](./MCP-USER-MANUAL.md#3-build-and-install).

---

## Connect the server (one-time)

```bash
# From the repo root — build once
mvn -DskipTests install

# Start the MCP server pointing at CLIDemo
cd Dist/release
./ingenious server mcp --project CLIDemo
```

Then wire your AI client to that process (see [Wiring guide](./MCP-USER-MANUAL.md#5-wiring-an-ai-client)). Once connected, every prompt below should just work.

---

## Section 1 — Explore what's already there
*Tools exercised: `project_list`, `project_info`, `scenario_list`, `scenario_info`, `testcase_list`, `testcase_show`*

### Prompt 1-A · Orientation
```
What INGenious projects are available? For each one, tell me how many 
scenarios and test cases it contains.
```
> The agent calls `ingenious_project_list`, then `ingenious_project_info` on each result.

---

### Prompt 1-B · Drill into a scenario
```
Show me everything inside the APIBasics scenario of CLIDemo — list all 
test cases with their step counts, then show me the full steps for GetUsers.
```
> Chain: `ingenious_scenario_info` → `ingenious_testcase_show`.

---

### Prompt 1-C · Understand the action vocabulary
```
What categories of actions are available? List all Browser actions and 
show me the full parameter list for the "Click" action.
```
> Chain: `ingenious_action_categories` → `ingenious_action_list` (category=Browser) → `ingenious_action_info` (name=Click).

---

### Prompt 1-D · Search for actions by intent
```
I need to verify that a JSON response field equals a specific value. 
Which action should I use and what arguments does it take?
```
> Agent calls `ingenious_action_search` (query="JSON field equals"), then `ingenious_action_info` on the match.

---

## Section 2 — Create your first test case
*Tools exercised: `scenario_create`, `testcase_create`, `testcase_add_step`, `testcase_validate`, `testcase_show`*

### Prompt 2-A · API test from scratch
```
In CLIDemo, create a new scenario called "HealthChecks". Inside it, 
create a test case called "PingAPI" that:
  1. Sets the endpoint to https://jsonplaceholder.typicode.com/users/1
  2. Sends a GET request
  3. Verifies the response code is 200
  4. Verifies the JSON field "name" equals "Leanne Graham"

Look up the correct action names before creating the steps.
```
> The agent discovers API actions first via `ingenious_action_search`, then calls `ingenious_testcase_create` with real action names, then `ingenious_testcase_validate` to confirm.

---

### Prompt 2-B · Browser test from scratch
```
Create a test case called "SearchGoogle" in the scenario "BrowserSamples" 
(create the scenario if it doesn't exist). The test should:
  1. Open a browser
  2. Navigate to https://www.google.com
  3. Type "INGenious test automation" into the search box
  4. Press Enter
  5. Wait for results to appear
  6. Verify that at least one result is visible
  7. Close the page

Use the correct INGenious browser action names.
```
> Agent: `ingenious_action_list` (Browser) → `ingenious_scenario_create` → `ingenious_testcase_create` → `ingenious_testcase_validate`.

---

### Prompt 2-C · Convert manual steps
```
I have these manual test steps for a login flow — convert them to a 
proper INGenious test case called "LoginHappy" in scenario "Auth":

1. Open Chrome
2. Go to https://demo.example.com/login
3. Enter "alice@example.com" in the username field
4. Enter "hunter2" in the password field
5. Click the Login button
6. Wait for the dashboard to load
7. Verify the welcome message says "Welcome, Alice"
8. Close the browser
```
> Uses the built-in `convert_manual_steps` prompt template: agent resolves each step to a real action name before writing.

---

### Prompt 2-D · Edit a test case step by step
```
In the "SearchGoogle" test case I just created, insert a step after step 3 
that waits for the search suggestions dropdown to be visible before pressing 
Enter. Then move the "Close page" step to be second-to-last.
```
> Agent: `ingenious_testcase_show` (read current steps) → `ingenious_testcase_insert_step` → `ingenious_testcase_move_step`.

---

## Section 3 — Object Repository
*Tools exercised: `object_list`, `object_show`, `object_search`, `object_add`, `object_update`, `object_delete`, `object_import_page`*

### Prompt 3-A · Explore the repository
```
List all Object Repository pages in CLIDemo. Then show me every locator 
defined on the first page. Is there anything matching "search" or "query"?
```
> Chain: `ingenious_object_list` → `ingenious_object_show` → `ingenious_object_search`.

---

### Prompt 3-B · Add locators manually
```
Add these locators to the CLIDemo Object Repository under a page called 
"GoogleSearchPage":
  - "search.input" — CSS selector: input[name='q']
  - "search.submit" — XPath: //input[@name='btnK']
  - "results.container" — CSS selector: #search
```
> Calls `ingenious_object_add` three times.

---

### Prompt 3-C · Scrape a live page
```
Open https://demo.testfire.net/login.jsp in a headless browser and 
import all the locators you find into a page called "AltitixLoginPage" 
in CLIDemo.
```
> Calls `ingenious_object_import_page` (requires Playwright CLI).

---

### Prompt 3-D · Update and clean up
```
In CLIDemo's Object Repository, find the locator "search.submit" on 
GoogleSearchPage and update it to use CSS selector: button[type=submit].
Then delete the "results.container" locator — we won't use it.
```
> Chain: `ingenious_object_update` → `ingenious_object_delete`.

---

## Section 4 — Data-driven testing
*Tools exercised: `env_list`, `env_create`, `data_sheet_create`, `data_column_add`, `data_row_add`, `data_set`, `data_get`, `data_show`, `data_generate`, `data_import`*

### Prompt 4-A · Inspect existing data
```
What environments exist in CLIDemo? Show me the contents of any data 
sheets that are already set up.
```
> Chain: `ingenious_env_list` → `ingenious_data_show` on each sheet.

---

### Prompt 4-B · Create a data sheet from scratch
```
Create a new data sheet called "LoginUsers" in CLIDemo with columns: 
username, password, expectedRole. Add three rows for the "dev" environment:
  Row 1: alice@example.com / pass1 / admin
  Row 2: bob@example.com / pass2 / editor
  Row 3: carol@example.com / pass3 / viewer
```
> Chain: `ingenious_data_sheet_create` → `ingenious_data_column_add` (×3) → `ingenious_data_row_add` (×3) → `ingenious_data_set` (×9).

---

### Prompt 4-C · Generate synthetic test data
```
Generate 20 rows of synthetic user data for a sheet called "SyntheticUsers" 
in CLIDemo. I need these columns: firstname, lastname, email, phone, city, 
and a random integer "age" between 18 and 65. Use seed 42 so results are 
reproducible.
```
> Calls `ingenious_data_generate` with typed column definitions.

---

### Prompt 4-D · Import from file + verify
```
I have a CSV file at /tmp/test_products.csv with columns: productId, name, 
price, category. Import it into CLIDemo as a data sheet called "Products" 
in the "staging" environment (create that environment first if needed), 
then show me the first 10 rows to confirm.
```
> Chain: `ingenious_env_create` → `ingenious_data_import` → `ingenious_data_show`.

---

## Section 5 — Archetype-driven generation
*Tools exercised: `gen_list`, `gen_testcase`, `gen_from_openapi`, `gen_from_har`, `import_curl`, `import_postman`, `import_playwright`*

### Prompt 5-A · Explore archetypes
```
List all available test archetypes. For the "browser-login" archetype, 
show me exactly what parameters I need to supply and what steps it will 
generate.
```
> Calls `ingenious_gen_list` (with and without category filter).

---

### Prompt 5-B · Generate from archetype
```
Use the "api-get" archetype to create a test case called "FetchPost" in 
scenario "GeneratedSuite" in CLIDemo. The target URL is 
https://jsonplaceholder.typicode.com/posts/1 and the expected status is 200.
After generating, validate the test case.
```
> Chain: `ingenious_gen_testcase` → `ingenious_testcase_validate`.

---

### Prompt 5-C · Preview first with dryRun
```
Preview what the "browser-login" archetype would generate for a test case 
called "LoginPreview" without actually creating any files. Show me the 
parameters needed and the steps that would be created.
```
> Calls `ingenious_gen_testcase` with `dryRun: true` — no files written.

---

### Prompt 5-D · Import a curl command
```
Convert this curl command into an INGenious test case called "CurlImport" 
in scenario "Imports":

  curl -X POST https://api.example.com/auth \
    -H "Content-Type: application/json" \
    -d '{"username":"alice","password":"secret"}'
```
> Calls `ingenious_import_curl`.

---

### Prompt 5-E · Import from OpenAPI spec
```
I have an OpenAPI spec at /tmp/petstore.yaml. Generate one test case per 
API operation in a new scenario called "PetstoreAPI" in CLIDemo. Set the 
base URL to https://petstore3.swagger.io/api/v3.
```
> Calls `ingenious_gen_from_openapi`.

---

### Prompt 5-F · Import a Postman/Bruno collection
```
I exported my Postman collection to /tmp/myapi.postman_collection.json. 
Import it into CLIDemo as scenario "PostmanImport", creating one test case 
per request.
```
> Calls `ingenious_import_postman` (or `ingenious_import_bruno` for Bruno format).

---

## Section 6 — Run tests & get results
*Tools exercised: `run`, `run_dry`, `run_async`, `run_status`, `run_logs`, `run_cancel`*

### Prompt 6-A · Dry run first
```
I want to run the "GetUsers" test case in scenario "APIBasics" of CLIDemo, 
but first do a dry run to check it's configured correctly and would actually 
execute. Then, if everything looks good, run it for real.
```
> Chain: `ingenious_run_dry` → assess result → `ingenious_run`.

---

### Prompt 6-B · Async run with status polling
```
Start running all test cases in the "APIBasics" scenario asynchronously 
using chromium. Give me a run ID so I can check the status. Then check 
the status every few seconds until it finishes.
```
> Chain: `ingenious_run_async` → `ingenious_run_status` (poll) → `ingenious_run_logs`.

---

### Prompt 6-C · Cancel a long-running test
```
I accidentally kicked off a run with ID "20240704-150000-abc123". 
Cancel it and show me how many steps had completed before cancellation.
```
> Chain: `ingenious_run_cancel` → `ingenious_run_logs`.

---

## Section 7 — Reporting & triage
*Tools exercised: `report_latest`, `report_history`, `report_failures`, `report_show`, `report_compare`, `report_export`*

### Prompt 7-A · What's the latest result?
```
What was the result of the last run for the "GetUsers" test case in 
CLIDemo? How long did it take and did anything fail?
```
> Calls `ingenious_report_latest`.

---

### Prompt 7-B · Failure deep-dive
```
The latest run of "APIBasics/GetUsers" had failures. Show me each 
failed step with its error message and, if available, a screenshot path. 
Suggest what might be wrong and how to fix it.
```
> Chain: `ingenious_report_failures` → `ingenious_testcase_show` → agent explains using `explain_failure` prompt.

---

### Prompt 7-C · Compare two runs
```
Compare the run from this morning (ID: 20240704-090000-old) with the 
run from this afternoon (ID: 20240704-150000-new) for CLIDemo. 
What test cases regressed? What got fixed? What stayed the same?
```
> Calls `ingenious_report_compare`.

---

### Prompt 7-D · Export for CI
```
Export the latest run report for CLIDemo in JUnit XML format so I can 
feed it into my Jenkins pipeline. Also export a CSV summary to /tmp/results.csv.
```
> Calls `ingenious_report_export` twice (format=junit and format=csv).

---

### Prompt 7-E · Trend analysis
```
Show me the run history for "APIBasics/GetUsers" over the last 10 runs. 
Is the pass rate improving or degrading? What's the average duration?
```
> Calls `ingenious_report_history`.

---

## Section 8 — Test sets (multi-case execution)
*Tools exercised: `testset_create`, `testset_add`, `testset_list`, `testset_show`*

### Prompt 8-A · Build a regression suite
```
Create a test set called "DailyRegression" under release "v2.0" in CLIDemo. 
Add every test case from the "APIBasics" scenario to it, all running with 
chromium. Then show me the full execution plan.
```
> Chain: `ingenious_testset_create` → `ingenious_testcase_list` → `ingenious_testset_add` (×N) → `ingenious_testset_show`.

---

### Prompt 8-B · Cross-browser execution plan
```
I want to run "APIBasics/GetUsers" in three browsers. Add it to a test set 
called "CrossBrowser" in release "v2.0" three times — once each for 
Chrome, Firefox, and Edge. Use separate iterations.
```
> Calls `ingenious_testset_add` three times with different browser values.

---

## Section 9 — Live browser authoring
*Tools exercised: `browser_session_start`, `browser_session_do`, `browser_session_snapshot`, `browser_session_save`, `browser_session_close`, `browser_inspect`*

### Prompt 9-A · Record a live session
```
Start a Playwright browser session in chromium. Navigate to 
https://demo.testfire.net, click on "Sign In", fill in username "admin" 
and password "admin", click the login button, then take a snapshot so I 
can see what the page looks like. Save the recorded steps as test case 
"LiveLogin" in scenario "Recorded".
```
> Chain: `ingenious_browser_session_start` → `ingenious_browser_session_do` (×4) → `ingenious_browser_session_snapshot` → `ingenious_browser_session_save` → `ingenious_browser_session_close`.

---

### Prompt 9-B · Inspect an element
```
I have a browser session open. Inspect the element at CSS selector 
"#account-summary" and tell me the best locator strategy to use — 
ID, CSS, or XPath? Add the best locator to the Object Repository 
page "DemoPage" as "account.summary".
```
> Chain: `ingenious_browser_inspect` → `ingenious_object_add`.

---

## Section 10 — Quality-of-life features (Phase 5)
*Tools exercised: Phase 5 flags across all write tools*

### Prompt 10-A · Preview before committing
```
Before creating anything, show me exactly what steps the "e2e-ui-then-api" 
archetype would generate for a test called "E2EPreview" in CLIDemo. 
I want to see the full step list without writing any files.
```
> `ingenious_gen_testcase` with `dryRun: true`.

---

### Prompt 10-B · Safe idempotent creation
```
Create test case "GetUsers" in scenario "APIBasics" of CLIDemo. I know 
it might already exist — if it does, just skip silently and tell me 
it's already there. Don't overwrite it.
```
> `ingenious_testcase_create` with `ifExists: "skip"`.

---

### Prompt 10-C · Recover from a typo
```
Show me the test case "GetAllUser" in scenario "APIBaiscs" of CLIDemo.
```
> The MCP server returns a rich error: `"Scenario not found: APIBaiscs. Did you mean: APIBasics?"` — agent then retries with the corrected name.

---

### Prompt 10-D · Health check the whole setup
```
Run a full health check on CLIDemo — check that the JDK is configured 
correctly, the browser drivers are available, Playwright CLI is installed, 
and the project structure is valid. Fix anything that can be auto-fixed.
```
> Calls `ingenious_doctor` and `ingenious_config_drivers`.

---

## Section 11 — Configuration management
*Tools exercised: `config_show`, `config_get`, `config_set`, `config_drivers`*

### Prompt 11-A · Read project config
```
Show me the full configuration for CLIDemo — what browser is configured 
by default, what environments are set up, and what driver paths are used?
```
> Chain: `ingenious_config_show` → `ingenious_config_drivers`.

---

### Prompt 11-B · Change a setting
```
In CLIDemo, change the default browser to Firefox and set the implicit 
wait timeout to 10 seconds. Show me the config before and after the change.
```
> Chain: `ingenious_config_get` → `ingenious_config_set` (×2) → `ingenious_config_show`.

---

## Section 12 — Full end-to-end workflow
*Covers ~55 tools in one conversation*

### Prompt 12 · The full gauntlet
```
Let's build a complete, runnable test suite for the JSONPlaceholder API 
from scratch in CLIDemo. Here's what I need:

1. Create a new scenario called "JSONPlaceholder"

2. Look up the right API action names for: setting an endpoint, sending 
   GET/POST requests, checking the response code, and extracting a JSON field.

3. Use the "api-get" archetype to generate a test case "GetPost1" that 
   fetches https://jsonplaceholder.typicode.com/posts/1 and expects 200.
   Preview it with dryRun first, then create it.

4. Create a test case "CreatePost" from scratch that:
   - POSTs to https://jsonplaceholder.typicode.com/posts
   - with body: {"title":"foo","body":"bar","userId":1}
   - verifies the response code is 201
   - verifies the JSON field "id" is not empty

5. Create a data sheet "PostIds" with columns: postId, expectedTitle.
   Generate 5 synthetic rows for the dev environment.

6. Build a test set "Regression" in release "v1.0" containing both 
   test cases, using chromium.

7. Run the test set with a dry run first, then actually execute it.

8. Show me the results — did both tests pass? If anything failed, 
   explain what went wrong and suggest a fix.

9. Export the report as JUnit XML to /tmp/jsonplaceholder-results.xml.
```
> This single prompt will exercise: `scenario_create`, `action_search`, `action_list`, `gen_list`, `gen_testcase` (dryRun + real), `testcase_create`, `testcase_validate`, `data_sheet_create`, `data_column_add`, `data_generate`, `testset_create`, `testset_add`, `run_dry`, `run`, `report_latest`, `report_failures`, `report_export`.

---

## Prompt reference card

| What you want to do | Sample prompt trigger |
|---------------------|----------------------|
| Explore the project | *"What scenarios and test cases are in CLIDemo?"* |
| Find the right action | *"What action do I use to verify a JSON field?"* |
| Author a test | *"Create a test case that logs in and checks the dashboard"* |
| Convert manual steps | *"Convert these manual test steps to INGenious actions"* |
| Add objects | *"Add these CSS selectors to the LoginPage"* |
| Scrape a page | *"Import all locators from https://…/login"* |
| Create test data | *"Generate 20 synthetic users with name, email, age"* |
| Use an archetype | *"Generate a browser-login test case from the template"* |
| Import a spec | *"Create tests from this OpenAPI/Postman/curl"* |
| Run tests | *"Run APIBasics/GetUsers and show me the result"* |
| See failures | *"What failed in the last run? What's the error?"* |
| Compare runs | *"How does today's run compare to yesterday's?"* |
| Export results | *"Export the report as JUnit XML for Jenkins"* |
| Live recording | *"Start a browser, navigate to …, click …, save as test case"* |
| Preview changes | *"Show me what would be created without actually creating it"* |
| Safe create | *"Create it if it doesn't exist, skip if it does"* |
| Health check | *"Check that drivers, JDK, and Playwright are working"* |

---

## Tips

**Always let the agent discover action names** — never guess. The golden rule:
> *"Look up the correct action name first, then create the test case."*

**Use dryRun to preview** any write operation before committing:
> *"Preview what would be created without writing any files."*

**Use ifExists=skip for idempotent workflows** in CI/CD pipelines:
> *"Create the test case — if it already exists, skip silently."*

**Prompt the built-in templates** by name for guided multi-step workflows:
- `create_test_case` — author from description
- `convert_manual_steps` — manual → automated
- `explain_failure` — parse and explain a test failure
- `debug_test` — walk through fragile steps
- `harden_test` — fix brittle locators and missing waits
- `triage_run` — compare runs and propose fixes
- `author_by_archetype` — generate from template
- `build_data_driven_suite` — parameterise with a data sheet
- `record_browser_test` — guided Playwright recording session
- `bootstrap_project` — create a new project with sample tests
- `suggest_locator` — suggest the best locator strategy
- `review_test_case` — best-practice audit
- `run_and_summarize` — run and explain results in one step

---

*For the full JSON-RPC reference, see [MCP-TEST-SCENARIOS.md](./MCP-TEST-SCENARIOS.md).*
*For complete tool documentation, see [MCP-USER-MANUAL.md](./MCP-USER-MANUAL.md).*
