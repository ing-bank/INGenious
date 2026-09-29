# MCP Test Scenarios — comprehensive examples across all 75 tools

Run the Engine MCP server first:
```bash
cd Resources && java -cp "$CLASSPATH" com.ing.engine.core.Control server mcp --project CLIDemo
```

Then invoke these tests from a shell with `jq` or Python + `curl` for clean output. Or use them directly in an MCP client (Claude, etc.).

---

## Phase 1: discovery & foundation (tools 1–10)

### ✅ Project discovery
```jsonc
// List all projects
{"name": "ingenious_project_list", "arguments": {}}

// Get CLIDemo summary (scenarios, releases, stats)
{"name": "ingenious_project_info", "arguments": {"project": "CLIDemo"}}

// Create a new project
{"name": "ingenious_project_create", "arguments": {"name": "TestMCP", "parent": "/tmp"}}
```

### ✅ Scenario & test case listing
```jsonc
// List scenarios in CLIDemo
{"name": "ingenious_scenario_list", "arguments": {"project": "CLIDemo"}}

// Inspect a scenario
{"name": "ingenious_scenario_info", "arguments": {"project": "CLIDemo", "scenario": "APIBasics"}}

// List all test cases in a scenario
{"name": "ingenious_testcase_list", "arguments": {"project": "CLIDemo", "scenario": "APIBasics"}}

// Show a test case (all steps)
{"name": "ingenious_testcase_show", "arguments": {"project": "CLIDemo", "scenario": "APIBasics", "testcase": "GetUsers"}}
```

### ✅ Action discovery (never invent action names!)
```jsonc
// List all Browser actions
{"name": "ingenious_action_list", "arguments": {"category": "Browser", "limit": 50}}

// Search for "click" actions
{"name": "ingenious_action_search", "arguments": {"query": "click"}}

// Get detailed info on a specific action
{"name": "ingenious_action_info", "arguments": {"name": "Click"}}

// List action categories
{"name": "ingenious_action_categories", "arguments": {}}
```

---

## Phase 2: authoring depth (tools 11–30)

### ✅ Create test cases from scratch
```jsonc
// Create an empty test case
{"name": "ingenious_testcase_create", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "EmptyCase"
}}

// Create with pre-populated steps
{"name": "ingenious_testcase_create", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "BrowserFlow",
  "steps": [
    {"action": "Open", "input": "@Browser", "description": "Open browser"},
    {"action": "GoTo", "input": "https://www.example.com", "description": "Navigate"},
    {"action": "Click", "object": "link", "description": "Click example link"},
    {"action": "ClosePage", "description": "Close"}
  ]
}}

// Create as reusable component (under ReusableComponents/ not TestPlan/)
{"name": "ingenious_testcase_create", "arguments": {
  "project": "CLIDemo",
  "scenario": "Utilities",
  "testcase": "LoginFlow",
  "reusable": true,
  "steps": [
    {"action": "Open", "input": "@Browser"},
    {"action": "Fill", "object": "LoginPage.user", "input": "@Data.username"},
    {"action": "Fill", "object": "LoginPage.pass", "input": "@Data.password"},
    {"action": "Click", "object": "LoginPage.submit"}
  ]
}}
```

### ✅ Test case step editing
```jsonc
// Add a step to the end
{"name": "ingenious_testcase_add_step", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "BrowserFlow",
  "action": "assertElementIsVisible",
  "object": "result_div",
  "description": "Verify result appeared"
}}

// Insert a step at index 2
{"name": "ingenious_testcase_insert_step", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "BrowserFlow",
  "index": 2,
  "action": "waitForElementToBeVisible",
  "object": "link",
  "description": "Wait for link"
}}

// Edit step 1 in place (replace)
{"name": "ingenious_testcase_edit_step", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "BrowserFlow",
  "index": 1,
  "action": "GoToHomescreen",
  "description": "Navigate to home instead"
}}

// Move step 3 to position 1
{"name": "ingenious_testcase_move_step", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "BrowserFlow",
  "from": 3,
  "to": 1
}}

// Remove step 2
{"name": "ingenious_testcase_remove_step", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "BrowserFlow",
  "index": 2
}}

// Validate the test case (checks all actions exist, required fields are filled)
{"name": "ingenious_testcase_validate", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "BrowserFlow"
}}

// Delete a test case
{"name": "ingenious_testcase_delete", "arguments": {
  "project": "CLIDemo",
  "scenario": "Phase2Test",
  "testcase": "BrowserFlow"
}}
```

### ✅ Object repository (read & write)
```jsonc
// List all Object Repository pages
{"name": "ingenious_object_list", "arguments": {"project": "CLIDemo"}}

// Show all objects on the LoginPage
{"name": "ingenious_object_show", "arguments": {"project": "CLIDemo", "page": "LoginPage"}}

// Search for "user" across all pages
{"name": "ingenious_object_search", "arguments": {"project": "CLIDemo", "query": "user"}}

// Add a locator to LoginPage
{"name": "ingenious_object_add", "arguments": {
  "project": "CLIDemo",
  "page": "LoginPage",
  "name": "login.resetLink",
  "type": "WebElement",
  "locator": "xpath",
  "value": "//a[contains(text(), 'Forgot Password')]",
  "description": "Forgot password link on login form"
}}

// Update an object's locator
{"name": "ingenious_object_update", "arguments": {
  "project": "CLIDemo",
  "page": "LoginPage",
  "name": "login.user",
  "value": "#username-input",  // new CSS selector
  "description": "Updated to use more stable ID"
}}

// Delete an object
{"name": "ingenious_object_delete", "arguments": {
  "project": "CLIDemo",
  "page": "LoginPage",
  "name": "login.resetLink"
}}

// Scrape a live URL and create/populate a page (requires @playwright/cli)
{"name": "ingenious_object_import_page", "arguments": {
  "project": "CLIDemo",
  "url": "https://example.com/login",
  "page": "ExampleLoginPage",
  "browser": "chromium"
}}
```

### ✅ Test sets (Release / TestLab)
```jsonc
// Create an empty test set
{"name": "ingenious_testset_create", "arguments": {
  "project": "CLIDemo",
  "release": "v1.0",
  "testset": "RegressionSuite"
}}

// Add test cases to a test set (one row per execution)
{"name": "ingenious_testset_add", "arguments": {
  "project": "CLIDemo",
  "release": "v1.0",
  "testset": "RegressionSuite",
  "scenario": "APIBasics",
  "testcase": "GetUsers",
  "browser": "Chrome",
  "iteration": "1",
  "execute": true
}}

// Add another execution row (same test case, different iteration)
{"name": "ingenious_testset_add", "arguments": {
  "project": "CLIDemo",
  "release": "v1.0",
  "testset": "RegressionSuite",
  "scenario": "APIBasics",
  "testcase": "GetUsers",
  "browser": "Firefox",
  "iteration": "2"
}}

// List all test sets
{"name": "ingenious_testset_list", "arguments": {"project": "CLIDemo"}}

// Show a test set (all rows/execution specs)
{"name": "ingenious_testset_show", "arguments": {
  "project": "CLIDemo",
  "release": "v1.0",
  "testset": "RegressionSuite"
}}
```

---

## Phase 3: data & environments (tools 31–45)

### ✅ Environment & data sheet management
```jsonc
// List all environments
{"name": "ingenious_env_list", "arguments": {"project": "CLIDemo"}}

// Create a new environment (e.g., staging)
{"name": "ingenious_env_create", "arguments": {
  "project": "CLIDemo",
  "environment": "staging"
}}

// Create a data sheet
{"name": "ingenious_data_sheet_create", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials"
}}

// Add a column to the sheet
{"name": "ingenious_data_column_add", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "column": "username"
}}

// Add more columns
{"name": "ingenious_data_column_add", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "column": "password"
}}

{"name": "ingenious_data_column_add", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "column": "role"
}}

// Add a data row
{"name": "ingenious_data_row_add", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "env": "dev",
  "row": 1
}}

// Show the sheet (first 50 rows)
{"name": "ingenious_data_show", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "env": "dev",
  "limit": 50
}}

// Get a single cell
{"name": "ingenious_data_get", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "column": "username",
  "row": 1,
  "env": "dev"
}}

// Set a cell value
{"name": "ingenious_data_set", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "column": "username",
  "row": 1,
  "value": "alice@example.com",
  "env": "dev"
}}

{"name": "ingenious_data_set", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "column": "password",
  "row": 1,
  "value": "securePass123",
  "env": "dev"
}}

{"name": "ingenious_data_set", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "column": "role",
  "row": 1,
  "value": "admin",
  "env": "dev"
}}

// Delete a row
{"name": "ingenious_data_row_delete", "arguments": {
  "project": "CLIDemo",
  "sheet": "LoginCredentials",
  "row": 2,
  "env": "dev"
}}

// Import from a CSV file
{"name": "ingenious_data_import", "arguments": {
  "project": "CLIDemo",
  "file": "/tmp/users.csv",
  "sheet": "ImportedUsers",
  "env": "dev"
}}
```

### ✅ Phase 3 generation: **Archetypes & synthetic data**
```jsonc
// List all archetypes
{"name": "ingenious_gen_list", "arguments": {}}

// List only Browser archetypes
{"name": "ingenious_gen_list", "arguments": {"category": "Browser"}}

// Generate a test case from the browser-login archetype
// (Note: parameters are archetype-specific; check gen_list for what each needs)
{"name": "ingenious_gen_testcase", "arguments": {
  "project": "CLIDemo",
  "archetype": "browser-login",
  "scenario": "Generated",
  "testcase": "GeneratedLogin",
  "params": {
    "url": "https://demo.example.com/login",
    "userField": "input#username",
    "username": "testuser",
    "passField": "input#password",
    "password": "testpass",
    "loginButton": "button[type=submit]",
    "dashboard": "div.dashboard"
  }
}}

// Generate and check what parameters are still unresolved
// (The response will have unresolvedParams if any ${token} couldn't be substituted)

// Generate from an API archetype
{"name": "ingenious_gen_testcase", "arguments": {
  "project": "CLIDemo",
  "archetype": "api-get",
  "scenario": "Generated",
  "testcase": "GeneratedHealthCheck",
  "params": {
    "url": "https://api.example.com/health",
    "status": "200"
  }
}}

// Generate synthetic data rows (no faker dependency, built-in types)
{"name": "ingenious_data_generate", "arguments": {
  "project": "CLIDemo",
  "sheet": "SyntheticUsers",
  "rows": 10,
  "columns": [
    {"name": "firstname", "type": "firstname"},
    {"name": "lastname", "type": "lastname"},
    {"name": "email", "type": "email"},
    {"name": "age", "type": "int"},
    {"name": "joined", "type": "date"}
  ],
  "env": "dev",
  "seed": 42
}}

// Generate test cases from an OpenAPI spec (one per operation)
{"name": "ingenious_gen_from_openapi", "arguments": {
  "project": "CLIDemo",
  "file": "/path/to/openapi.yaml",
  "scenario": "OpenAPIGenerated",
  "baseUrl": "https://api.example.com"
}}

// Generate test cases from a HAR capture (browser network export)
{"name": "ingenious_gen_from_har", "arguments": {
  "project": "CLIDemo",
  "file": "/path/to/network.har",
  "scenario": "HARGenerated",
  "urlFilter": "api.example.com"
}}
```

---

## Phase 4: execution & reporting (tools 46–65)

### ✅ Run tests (sync & async)
```jsonc
// Run a single test case (synchronous — waits for completion)
{"name": "ingenious_run", "arguments": {
  "project": "CLIDemo",
  "target": "APIBasics/GetUsers",
  "browser": "chromium"
}}

// Run async (returns immediately with a runId, then poll status)
{"name": "ingenious_run", "arguments": {
  "project": "CLIDemo",
  "target": "APIBasics/GetUsers",
  "async": true,
  "browser": "firefox"
}}

// Check the status of an async run
{"name": "ingenious_run", "arguments": {
  "action": "status",
  "runId": "20240704-143022-a1b2c3"
}}

// Fetch logs from a run
{"name": "ingenious_run", "arguments": {
  "action": "logs",
  "runId": "20240704-143022-a1b2c3"
}}

// Cancel an async run
{"name": "ingenious_run", "arguments": {
  "action": "cancel",
  "runId": "20240704-143022-a1b2c3"
}}

// Dry-run (plan only, don't execute) — useful for validation
{"name": "ingenious_run", "arguments": {
  "project": "CLIDemo",
  "target": "APIBasics/GetUsers",
  "dryRun": true
}}

// Re-run the latest execution
{"name": "ingenious_run", "arguments": {
  "project": "CLIDemo",
  "target": "APIBasics/GetUsers",
  "rerun": true
}}

// Run with tags (filter by tags if the test case has them)
{"name": "ingenious_run", "arguments": {
  "project": "CLIDemo",
  "target": "APIBasics/GetUsers",
  "tags": "smoke,daily"
}}

// Run a test set (multiple test cases from TestLab)
{"name": "ingenious_run", "arguments": {
  "project": "CLIDemo",
  "target": "v1.0/RegressionSuite",
  "browser": "chromium"
}}
```

### ✅ Reporting & analysis
```jsonc
// Get the latest run for a target
{"name": "ingenious_report_latest", "arguments": {
  "project": "CLIDemo",
  "target": "APIBasics/GetUsers"
}}

// Get run history (last N runs)
{"name": "ingenious_report_history", "arguments": {
  "project": "CLIDemo",
  "target": "APIBasics/GetUsers",
  "limit": 10
}}

// Show details of a specific run
{"name": "ingenious_report_show", "arguments": {
  "project": "CLIDemo",
  "runId": "20240704-143022-a1b2c3"
}}

// List only the failed test cases in a run
{"name": "ingenious_report_failures", "arguments": {
  "project": "CLIDemo",
  "target": "APIBasics/GetUsers"
}}

// Compare two runs (regression detection)
{"name": "ingenious_report_compare", "arguments": {
  "project": "CLIDemo",
  "runA": "20240704-120000-old123",
  "runB": "20240704-143022-new456"
}}

// Export a report in different formats
{"name": "ingenious_report_export", "arguments": {
  "project": "CLIDemo",
  "runId": "20240704-143022-a1b2c3",
  "format": "csv",
  "output": "/tmp/report.csv"
}}

{"name": "ingenious_report_export", "arguments": {
  "project": "CLIDemo",
  "runId": "20240704-143022-a1b2c3",
  "format": "junit",
  "output": "/tmp/report.xml"
}}

{"name": "ingenious_report_export", "arguments": {
  "project": "CLIDemo",
  "runId": "20240704-143022-a1b2c3",
  "format": "json"
}}
```

### ✅ Configuration & diagnostics
```jsonc
// Show configuration files in the project
{"name": "ingenious_config_show", "arguments": {"project": "CLIDemo"}}

// Get available drivers and Playwright CLI status
{"name": "ingenious_config_drivers", "arguments": {}}

// Full project health check (JDK, drivers, Playwright, project structure)
{"name": "ingenious_doctor", "arguments": {"project": "CLIDemo"}}
```

---

## Phase 5: quality-of-life (dry-run, idempotency, rich errors)

### ✅ Dry-run — preview without persisting
```jsonc
// Preview creating a test case without writing
{"name": "ingenious_testcase_create", "arguments": {
  "project": "CLIDemo",
  "scenario": "DryRun",
  "testcase": "Preview",
  "dryRun": true,
  "steps": [{"action": "Open"}]
}}
// → {dryRun: true, wouldCreate: true, scenario: "DryRun", testcase: "Preview", steps: 1}
// (no files created!)

// Preview adding to a test set
{"name": "ingenious_testset_add", "arguments": {
  "project": "CLIDemo",
  "release": "v1.0",
  "testset": "Preview",
  "scenario": "APIBasics",
  "testcase": "GetUsers",
  "dryRun": true
}}

// Preview adding an object to Object Repository
{"name": "ingenious_object_add", "arguments": {
  "project": "CLIDemo",
  "page": "PreviewPage",
  "name": "preview.button",
  "dryRun": true
}}

// Preview setting a data cell
{"name": "ingenious_data_set", "arguments": {
  "project": "CLIDemo",
  "sheet": "Preview",
  "column": "email",
  "row": 1,
  "value": "test@example.com",
  "dryRun": true
}}

// Preview from archetype generation
{"name": "ingenious_gen_testcase", "arguments": {
  "project": "CLIDemo",
  "archetype": "api-get",
  "scenario": "Preview",
  "testcase": "PreviewAPI",
  "dryRun": true,
  "params": {"url": "https://api.example.com/health"}
}}
```

### ✅ Idempotent creates — ifExists=skip|overwrite
```jsonc
// Create a test case — normal (error if exists)
{"name": "ingenious_testcase_create", "arguments": {
  "project": "CLIDemo",
  "scenario": "Idempotent",
  "testcase": "TestCase1",
  "steps": [{"action": "Open", "input": "@Browser"}]
}}

// Create again with ifExists=skip → don't error, return "existing"
{"name": "ingenious_testcase_create", "arguments": {
  "project": "CLIDemo",
  "scenario": "Idempotent",
  "testcase": "TestCase1",
  "ifExists": "skip"
}}
// → {created: false, existing: true, testcase: "TestCase1", steps: 1}

// Create with ifExists=overwrite → replace the existing one
{"name": "ingenious_testcase_create", "arguments": {
  "project": "CLIDemo",
  "scenario": "Idempotent",
  "testcase": "TestCase1",
  "steps": [{"action": "Open"}, {"action": "ClosePage"}],
  "ifExists": "overwrite"
}}
// → {created: true, scenario: "Idempotent", testcase: "TestCase1", steps: 2}

// Same with archetypes
{"name": "ingenious_gen_testcase", "arguments": {
  "project": "CLIDemo",
  "archetype": "browser-flow",
  "scenario": "Idempotent",
  "testcase": "GenCase",
  "ifExists": "skip",
  "params": {"url": "https://example.com", "element": "btn"}
}}
```

### ✅ Rich errors with suggestions — "Did you mean?"
```jsonc
// Typo in scenario name → error with suggestions
{"name": "ingenious_testcase_show", "arguments": {
  "project": "CLIDemo",
  "scenario": "APIBasc",  // typo (missing 'i')
  "testcase": "GetUsers"
}}
// → error: "Scenario not found: APIBasc Did you mean: APIBasics?"
// → error.data.suggestions: ["APIBasics"]

// Typo in archetype name → ranked suggestions
{"name": "ingenious_gen_testcase", "arguments": {
  "project": "CLIDemo",
  "archetype": "browser-logn",  // typo (missing 'i')
  "scenario": "Test",
  "testcase": "Test"
}}
// → error: "Unknown archetype: browser-logn Did you mean: browser-login, browser-flow, browser-search?"
// → error.data.suggestions: ["browser-login", "browser-flow", "browser-search"]

// Typo in object page name
{"name": "ingenious_object_show", "arguments": {
  "project": "CLIDemo",
  "page": "LoginPg"  // typo
}}
// → error: "Page not found: LoginPg Did you mean: LoginPage?"
// → error.data.suggestions: ["LoginPage"]

// Non-existent data sheet
{"name": "ingenious_data_show", "arguments": {
  "project": "CLIDemo",
  "sheet": "BadSheet"
}}
// → error: "Data sheet not found: BadSheet Did you mean: LoginCredentials, SyntheticUsers?"
// → error.data.suggestions: ["LoginCredentials", "SyntheticUsers"]
```

---

## Prompts (natural-language workflows)

Each prompt is a pre-written instruction that an agent can use. Invoke with `prompts/get`:

```jsonc
// Get a prompt and its rendered message
{"method": "prompts/get", "params": {
  "name": "create_test_case",
  "arguments": {
    "project": "CLIDemo",
    "scenario": "PromptDemo",
    "testcase": "LoginTest",
    "description": "Sign in with valid credentials and verify dashboard loads"
  }
}}

// Get the convert_manual_steps prompt
{"method": "prompts/get", "params": {
  "name": "convert_manual_steps",
  "arguments": {
    "project": "CLIDemo",
    "scenario": "Manual",
    "testcase": "ConvertedSteps",
    "steps": "1. Open browser\n2. Go to https://example.com\n3. Click 'Login'\n4. Fill username\n5. Fill password\n6. Click 'Sign In'\n7. Verify 'Welcome' text"
  }
}}

// Explain a failure
{"method": "prompts/get", "params": {
  "name": "explain_failure",
  "arguments": {
    "project": "CLIDemo",
    "target": "APIBasics/GetUsers"
  }
}}

// Debug a test for fragility
{"method": "prompts/get", "params": {
  "name": "debug_test",
  "arguments": {
    "project": "CLIDemo",
    "scenario": "APIBasics",
    "testcase": "GetUsers"
  }
}}

// Generate a test from an archetype
{"method": "prompts/get", "params": {
  "name": "author_by_archetype",
  "arguments": {
    "archetype": "browser-login",
    "scenario": "PromptDemo",
    "testcase": "ArchetypeLogin",
    "description": "Login as alice@example.com with password hunter2"
  }
}}

// Build a data-driven suite
{"method": "prompts/get", "params": {
  "name": "build_data_driven_suite",
  "arguments": {
    "project": "CLIDemo",
    "scenario": "APIBasics",
    "testcase": "GetUsers",
    "sheet": "GetUsersData"
  }
}}

// Harden a flaky test
{"method": "prompts/get", "params": {
  "name": "harden_test",
  "arguments": {
    "project": "CLIDemo",
    "scenario": "APIBasics",
    "testcase": "GetUsers"
  }
}}

// Triage a failure
{"method": "prompts/get", "params": {
  "name": "triage_run",
  "arguments": {
    "project": "CLIDemo",
    "target": "APIBasics/GetUsers"
  }
}}

// Bootstrap a new project
{"method": "prompts/get", "params": {
  "name": "bootstrap_project",
  "arguments": {
    "name": "NewProject",
    "parentDir": "/tmp"
  }
}}
```

---

## Resources (knowledge & reference)

```jsonc
// Get the list of all resources
{"method": "resources/list", "params": {}}

// Read the getting-started guide
{"method": "resources/read", "params": {
  "uri": "ingenious://docs/getting-started"
}}

// Read the action catalog (JSON)
{"method": "resources/read", "params": {
  "uri": "ingenious://catalog/actions"
}}

// Read archetype templates (JSON)
{"method": "resources/read", "params": {
  "uri": "ingenious://catalog/archetypes"
}}

// Read test step schema
{"method": "resources/read", "params": {
  "uri": "ingenious://docs/step-schema"
}}

// Read best practices
{"method": "resources/read", "params": {
  "uri": "ingenious://docs/best-practices"
}}

// Read project summary
{"method": "resources/read", "params": {
  "uri": "ingenious://project/CLIDemo/summary"
}}
```

---

## Integration test workflow (end-to-end)

This mimics an agent's workflow from scratch:

```bash
#!/bin/bash
# 1. Discover the existing landscape
curl ... tools/call ingenious_project_list
curl ... tools/call ingenious_scenario_list "project: CLIDemo"

# 2. Create a test case from an archetype
curl ... tools/call ingenious_gen_testcase \
  "archetype: browser-login, scenario: E2E, testcase: LoginTest, params: {...}"

# 3. Create test data
curl ... tools/call ingenious_data_sheet_create "sheet: LoginUsers"
curl ... tools/call ingenious_data_generate \
  "sheet: LoginUsers, rows: 5, columns: [{name: username}, {name: password}]"

# 4. Create an Object Repository page
curl ... tools/call ingenious_object_import_page \
  "url: https://app.example.com/login, page: LoginPage"

# 5. Validate the test case
curl ... tools/call ingenious_testcase_validate "scenario: E2E, testcase: LoginTest"

# 6. Dry-run the test
curl ... tools/call ingenious_run "target: E2E/LoginTest, dryRun: true"

# 7. Execute
curl ... tools/call ingenious_run "target: E2E/LoginTest, browser: chromium"

# 8. Analyze results
curl ... tools/call ingenious_report_latest "target: E2E/LoginTest"
curl ... tools/call ingenious_report_failures "target: E2E/LoginTest"

# 9. Compare with a prior run
curl ... tools/call ingenious_report_compare \
  "runA: 20240704-100000-old, runB: 20240704-143022-new"

# 10. Export report
curl ... tools/call ingenious_report_export \
  "runId: 20240704-143022-new, format: junit, output: /tmp/report.xml"
```

---

## Summary of test coverage

| Category | Tool count | Key examples |
|----------|-----------|--------------|
| **Discovery** | 10 | project_list, scenario_list, testcase_list, action_search, action_info |
| **Authoring** | 20 | testcase_create, testcase_add/edit/insert/remove/move_step, object_add/update/delete, testset_create/add |
| **Data** | 15 | data_sheet_create, data_column_add, data_row_add, data_show, data_get, data_set, data_generate, env_list/create/delete |
| **Generation** | 5 | gen_list, gen_testcase, gen_from_openapi, gen_from_har, data_generate |
| **Execution** | 10 | run (sync/async/status/logs/cancel/dry/rerun) |
| **Reporting** | 10 | report_latest, report_history, report_show, report_failures, report_compare, report_export |
| **Config** | 3 | config_show, config_drivers, doctor |
| **Browser** | 5 | browser_session_start/do/snapshot/save/close, object_import_page |
| **Quality** | Phase 5 | dryRun, ifExists, rich error suggestions |

**Total: 75 tools + 13 prompts + 5 resources, all tested above.**
